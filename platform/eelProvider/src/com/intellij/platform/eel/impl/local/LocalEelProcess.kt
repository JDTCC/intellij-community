// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.eel.impl.local

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.platform.eel.EelApi
import com.intellij.platform.eel.EelProcess
import com.intellij.platform.eel.KillableProcess
import com.intellij.platform.eel.impl.local.processKiller.PosixProcessKiller
import com.intellij.platform.eel.impl.local.processKiller.WinProcessKiller
import com.intellij.platform.util.coroutines.childScope
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.IOException

internal class LocalEelProcess(
  private val process: Process,
  private val killer: KillableProcess = if (SystemInfoRt.isWindows) WinProcessKiller(process) else PosixProcessKiller(process),
) : EelProcess, KillableProcess by killer {

  private val scope: CoroutineScope = ApplicationManager.getApplication().service<ExecLocalProcessService>().scope()

  override val pid: EelApi.Pid = LocalPid(process.pid())
  override val stdin: SendChannel<ByteArray> = StreamWrapper.OutputStreamWrapper(scope, process.outputStream).connectChannel()
  override val stdout: ReceiveChannel<ByteArray> = StreamWrapper.InputStreamWrapper(scope, process.inputStream).connectChannel()
  override val stderr: ReceiveChannel<ByteArray> = StreamWrapper.InputStreamWrapper(scope, process.errorStream).connectChannel()
  override val exitCode: Deferred<Int> = scope.async {
    process.awaitExit()
  }

  override suspend fun sendStdinWithConfirmation(data: ByteArray) {
    withContext(Dispatchers.IO) {
      try {
        with(process.outputStream) {
          write(data)
          flush()
        }
      }
      catch (_: IOException) {
        // TODO: Check that stream is indeed closed.
        if (process.isAlive) {
          throw EelProcess.SendStdinError.StdinClosed()
        }
        else {
          throw EelProcess.SendStdinError.ProcessExited()
        }
      }
    }
  }

  override fun convertToJavaProcess(): Process = process

  override suspend fun resizePty(columns: Int, rows: Int) {
    TODO("Not yet implemented. Use Pty4J")
  }
}

@Service
private class ExecLocalProcessService(private val scope: CoroutineScope) {
  fun scope(): CoroutineScope = scope.childScope("ExecLocalProcessService")
}
// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4idea.branch

import com.intellij.testFramework.LightVirtualFile
import git4idea.GitBranch
import git4idea.GitLocalBranch
import git4idea.GitStandardRemoteBranch
import git4idea.branch.GitBranchesTreeTestContext.Companion.ORIGIN
import git4idea.branch.GitBranchesTreeTestContext.Companion.ORIGIN_URLS
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.test.MockGitRepository
import git4idea.ui.branch.dashboard.BranchInfo

class GitBranchesTreeStructureTest : GitBranchesTreeTest() {
  private lateinit var repo1: GitRepository
  private lateinit var repo2: GitRepository

  override fun setUp() {
    super.setUp()
    repo1 = MockGitRepository(project, LightVirtualFile("repo-1"))
    repo2 = MockGitRepository(project, LightVirtualFile("repo-2"))
  }

  fun `test remote and local nodes are missing if have no children`() = branchesTreeTest(groupByDirectories = false) {
    setState(localBranches = listOf("qqq"), remoteBranches = emptyList(), expanded = true)
    assertTree("""
      |-ROOT
      | HEAD
      | -LOCAL
      |  BRANCH:qqq
    """.trimMargin())

    setState(localBranches = emptyList(), remoteBranches = listOf("qqq"), expanded = true)
    assertTree("""
      |-ROOT
      | HEAD
      | -REMOTE
      |  BRANCH:origin/qqq
    """.trimMargin())
  }

  fun `test remote grouped by origin`() = branchesTreeTest {
    setRawState(
      localBranches = listOf(),
      remoteBranches = listOf(
        branchInfo(GitStandardRemoteBranch(NOT_ORIGIN, "a-branch")),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "main")),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "branch")),
        branchInfo(GitStandardRemoteBranch(NOT_ORIGIN, "group/branch")),
        branchInfo(GitStandardRemoteBranch(NOT_ORIGIN, "Group/branch-1")),
      ),
      expanded = true
    )

    assertTree("""
      |-ROOT
      | HEAD
      | -REMOTE
      |  -REMOTE:not-origin
      |   -GROUP:Group
      |    BRANCH:not-origin/Group/branch-1
      |   -GROUP:group
      |    BRANCH:not-origin/group/branch
      |   BRANCH:not-origin/a-branch
      |  -REMOTE:origin
      |   BRANCH:origin/branch
      |   BRANCH:origin/main
    """.trimMargin())
  }

  fun `test group by directory tree structure with favorites`() = branchesTreeTest(groupByDirectories = false) {
    setRawState(
      localBranches = listOf(
        branchInfo(GitLocalBranch("aaaa")),
        branchInfo(GitLocalBranch("favorite"), isFavorite = true),
        branchInfo(GitLocalBranch("group/favorite"), isFavorite = true),
        branchInfo(GitLocalBranch("a-group/aaaa")),
      ),
      remoteBranches = listOf(
        branchInfo(GitStandardRemoteBranch(ORIGIN, "aa")),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "favorite"), isFavorite = true),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "group/favorite"), isFavorite = true),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "a-group/aaaa")),
      ),
      expanded = true,
    )

    assertTree("""
      |-ROOT
      | HEAD
      | -LOCAL
      |  BRANCH:favorite
      |  BRANCH:group/favorite
      |  BRANCH:a-group/aaaa
      |  BRANCH:aaaa
      | -REMOTE
      |  BRANCH:origin/favorite
      |  BRANCH:origin/group/favorite
      |  BRANCH:origin/a-group/aaaa
      |  BRANCH:origin/aa
    """.trimMargin())
  }

  fun `test simple group by repos`() = branchesTreeTest(groupByDirectories = false, groupByRepos = true) {
    setRawState(
      localBranches = listOf(
        branchInfo(GitLocalBranch("main"), repositories = listOf(repo1))
      ),
      remoteBranches = listOf(
        branchInfo(GitStandardRemoteBranch(NOT_ORIGIN, "a-branch"), repositories = listOf(repo1, repo2)),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "a-branch"), repositories = listOf(repo2)),
      ),
      expanded = true,
    )

    assertTree("""
      |-ROOT
      | HEAD
      | -LOCAL
      |  -REPO:/repo-1
      |   BRANCH:main
      | -REMOTE
      |  -REPO:/repo-1
      |   BRANCH:not-origin/a-branch
      |  -REPO:/repo-2
      |   BRANCH:not-origin/a-branch
      |   BRANCH:origin/a-branch
    """.trimMargin())
  }

  fun `test multiple repos without grouping`() = branchesTreeTest(groupByDirectories = false) {
    setRawState(
      localBranches = listOf(
        branchInfo(GitLocalBranch("main"), repositories = listOf(repo1))
      ),
      remoteBranches = listOf(
        branchInfo(GitStandardRemoteBranch(NOT_ORIGIN, "a-branch"), repositories = listOf(repo1, repo2)),
        branchInfo(GitStandardRemoteBranch(ORIGIN, "a-branch"), repositories = listOf(repo2)),
      ),
      expanded = true,
    )

    assertTree("""
      |-ROOT
      | HEAD
      | -LOCAL
      |  BRANCH:main
      | -REMOTE
      |  BRANCH:not-origin/a-branch
      |  BRANCH:origin/a-branch
    """.trimMargin())
  }

  private companion object {
    val NOT_ORIGIN = GitRemote("not-origin", ORIGIN_URLS, ORIGIN_URLS, listOf(), listOf())

    fun branchInfo(branch: GitBranch, isCurrent: Boolean = false, isFavorite: Boolean = false, repositories: List<GitRepository> = emptyList()) =
      BranchInfo(branch, isCurrent, isFavorite, repositories = repositories)
  }
}
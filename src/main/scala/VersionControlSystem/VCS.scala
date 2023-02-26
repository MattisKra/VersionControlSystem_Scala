package VersionControlSystem

import VersionControlSystem.Commit.generateIdentifier
import VersionControlSystem.VersionHistory.saveVersionHistory

import java.io.{BufferedReader, File, FileInputStream, FileWriter}
import scala.collection.mutable.Stack

/*
  Class containing the main functionality of the Version Control System.
  The parameter 'sourcePath' specifies the root of the repository.
*/
class VCS(val sourcePath : String):
  var versionHistory : VersionHistory = VersionHistory.loadVersionHistory(sourcePath + "/.vcss/")
  private var currentCommit : Commit = if (versionHistory != null) versionHistory.currentCommit else null
  private val stagingArea : collection.mutable.Set[String] = collection.mutable.Set()

  /*
    Creates the folder structure
  */
  def initializeVCS() : Unit =
  {
    // Create version history if doesn't already exist
    val versionHistoryFile : File = new File(sourcePath + "/.vcss")
    if(versionHistoryFile.exists())
    {
      println("Repository already initialized!")
      return
    }

    // Create commit directory
    new File(sourcePath + "/.vcss/commits").mkdirs()
    val commit : Commit = Commit(Array(), StructureDiff(sourcePath, null), null)
    commit.isHead = true
    Commit.saveToFile(commit, sourcePath + "/.vcss/commits/", commit.identifier)
    if (versionHistory == null) versionHistory = VersionHistory()

    versionHistory.commitChanges(commit)

    
    println("Initialized vcss repository.")


    VersionHistory.saveVersionHistory(versionHistory, sourcePath + "/.vcss/")
  }

  /*
    prints the structural differences in the working copy
    e.g. when a file or directory was added
  */
  def status() : Unit =
  {
    if(currentCommit == null)
    {
//      val fileDiffs: List[FileDiff] = currentCommit.fileDiffs
      val dummyStructureDiff : StructureDiff = StructureDiff(sourcePath, null)
      val structureDiff: StructureDiff = StructureDiff.generateDiff(dummyStructureDiff)

      print(structureDiff.getString())
    }
    else {
      val fileDiffs: Array[FileDiff] = currentCommit.fileDiffs
      val structureDiff: StructureDiff = StructureDiff.generateDiff(currentCommit.structureDiff)
      StructureDiff.generateDiff(currentCommit.structureDiff)

      print(structureDiff.getString())
    }

  }

  /*
    Adds all changed files which are either directly specified or within a specified directory to the
    staging area.
  */
  def stage(ps : Array[String]) : Unit =
  {
    var paths : Array[String] = ps

    if(paths(0) == "*") // Add all
      paths = new File(sourcePath).listFiles().map(f => f.getAbsolutePath)

    for
      path <- paths
    do
      val data = new File(path)

      if(data.exists())
      {
        if(data.isDirectory) // Add all files in directory to staging area
        {
          val unsearched: Stack[File] = Stack(data)
          while (unsearched.nonEmpty) {
            if (unsearched.top.isFile)
              stagingArea.add(unsearched.pop.getPath)
            else if (unsearched.top.isDirectory) {
              unsearched.pushAll(unsearched.pop.listFiles())
            }
            else
              unsearched.pop()
          }
        }
        else if(data.isFile)
        {
          stagingArea.add(data.getPath)
        }
      }

      println("Staged the following files for commit:")
      println(stagingArea)

  }

  /*
    save changes of current staging area as a commit
  */
  def commitChanges() =
  {
    println(currentCommit)
    val commitDirectory : String = sourcePath + "/.vcss/commits/"
    var fileDiffs : Array[FileDiff] = Array()

    for(path <- stagingArea) {
      val fileDiff : FileDiff = FileDiff(path, if(currentCommit != null )currentCommit.getDiffForFile(path) else null)
      fileDiff.generateDiff()
      fileDiffs = fileDiffs :+ fileDiff
    }

    val dummyStructureDiff : StructureDiff = StructureDiff(sourcePath, null)
    val structureDiff : StructureDiff = StructureDiff.generateDiff(if(currentCommit != null)
                                                                   currentCommit.structureDiff else dummyStructureDiff)

    val commit : Commit = Commit(fileDiffs, structureDiff, currentCommit)
    Commit.saveToFile(commit, commitDirectory, commit.identifier)
    versionHistory.commitChanges(commit)
    println(commit.identifier)
    VersionHistory.saveVersionHistory(versionHistory, sourcePath + "/.vcss/")
  }

  /*

  */
  def checkoutVersion(commitNumber : String) =
  {
    if (currentCommit == null) println("no commits yet")
    else{
      var temp : Commit = currentCommit
      var found : Boolean = false
      while(!found)

        if (generateIdentifier(temp) == commitNumber){
          found = true
          currentCommit = temp
        }

        if (temp.isHead){
          println("Commit number doesn't exist")
          found = true
        }
        else{
          temp = temp.previousCommit
        }
    }
  }

  /*
  DEBUG FEATURE
  */
  def testFeature(args : Array[String]) =
  {
    val commit : Commit = Commit.loadFromFile(sourcePath + "/.vcss/commits/", currentCommit.identifier)
    println(commit)
  }
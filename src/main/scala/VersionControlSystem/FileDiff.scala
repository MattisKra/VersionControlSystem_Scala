package VersionControlSystem

import java.io.{BufferedReader, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, StringReader}

/*
  Class representing the change of a single file between two versions.
*/
class FileDiff(val sourcePath : String, val previousDiff : FileDiff = null) extends Serializable:
  private var changes: List[(Int, Operation, String)] = List()

  /*
    generates a list of of differences in a File to the previous Diff based on the operation
    this function does not change class attribute changes
  */
  def generateDiff() : Unit =
  {
//    println("Reading file: " + sourcePath)
    val bRFile : BufferedReader = io.Source.fromFile(sourcePath).bufferedReader()
    val prevVersion : StringBuffer = generateVersion(previousDiff)
    val bRPrev : BufferedReader = BufferedReader(StringReader(prevVersion.toString))
    var index: Int = 0
    var stringA: String = ""
    var stringB: String = ""

    while (stringA != null) {
      bRFile.mark(9999) // Practical limit should be tested (number of chars!!!)
      stringA = bRPrev.readLine()
      stringB = bRFile.readLine()
      index = index + 1

      if (stringA != stringB) {
        var tempContent: List[String] = List()

        while (stringA != stringB && stringB != null /*&& tempContent.length < 20*/) // Abort if temporary list exceeds certain size
        {
          tempContent = tempContent :+ stringB
          stringB = bRFile.readLine()
        }

        if (stringB == null && stringA != null) // Deletion or cutoff
        {
          addChange(index, Operation.Deletion, stringA)
          bRFile.reset()
        }
        else // Insertion
        {
          addChange(index, Operation.Insertion, tempContent.mkString("\n"))
        }
      }
    }
  }

  /*
    Generates version by applying all previous diffs recursively
  */
  def generateVersion(fileDiff : FileDiff) : StringBuffer =
  {
    if(fileDiff == null)
    {
      return new StringBuffer()
    }
    else
    {
      val previousContent : String = generateVersion(fileDiff.previousDiff).toString
      val bufferedReader : BufferedReader = BufferedReader(StringReader(previousContent))

      val stringBuffer: StringBuffer = new StringBuffer()

      var changes: List[(Int, Operation, String)] = fileDiff.getChanges()

      if(changes == null || changes.isEmpty)
        return new StringBuffer(previousContent)

      var (index, operation, content): (Int, Operation, String) = changes.head
      changes = changes.tail

      var lineNumber: Int = 0
      var lineContent: String = ""

      while (lineContent != null || lineNumber <= index) {
        // Add new content if at index and operation is insertion
        if (lineNumber == index && operation == Operation.Insertion)
          stringBuffer.append(content + "\n")

        // Add old content if not yet at index or operation is deletion (but not if first line)
        if ((lineNumber != index || operation != Operation.Deletion) && lineNumber != 0 && lineContent != null)
          stringBuffer.append(lineContent + "\n")

        // Load next change
        if (lineNumber == index) {
          // Load next line
          lineNumber += 1
          lineContent = bufferedReader.readLine()

          index = if (changes.nonEmpty) changes.head._1 else -1
          operation = if (changes.nonEmpty) changes.head._2 else Operation.Deletion
          content = if (changes.nonEmpty) changes.head._3 else ""
          changes = if (changes.nonEmpty) changes.tail else changes
        }
        else {
          lineNumber += 1
          lineContent = bufferedReader.readLine()
        }
      }

      return stringBuffer
    }
  }

  /*
    adds a generated difference to the class attribute
    depending on the parameters either for a single Change or for a List of changes

  */
  def addChange(index : Int, operation: Operation, content : String) : Unit =
  {
    changes = changes :+ (index, operation, content)
  }

  // returns changes as a string
  def getString() : String =
  {
    changes.mkString("\n")
  }

  def getChanges() : List[(Int, Operation, String)] =
  {
    changes
  }
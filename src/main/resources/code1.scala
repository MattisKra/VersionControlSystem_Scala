import VersionControlSystem.VCS
@main
def main(): Unit = {
  val vcs: VCS = VCS()

  vcs.generateDiffForFile("/Text1.txt", "/Text2.txt")
  vcs.generateDiffForFile("/code1.scala", "/code2.scala")

  print("1")

  print("2")
}
export class Logs {
  static sortLevels = (levels: string[]) => {
    return levels.sort((l1, l2) => {
      const level1 = l1.toUpperCase()
      const level2 = l2.toUpperCase()
      if (level1 == level2) return 0
      if (level1 == "TRACE") return -1
      if (level2 == "TRACE") return 1
      if (level1 == "DEBUG") return -1
      if (level2 == "DEBUG") return 1
      if (level1 == "INFO") return -1
      if (level2 == "INFO") return 1
      if (level1 == "WARN") return -1
      if (level2 == "WARN") return 1
      if (level1 == "WARNING") return -1
      if (level2 == "WARNING") return 1
      if (level1 == "ERROR") return -1
      if (level2 == "ERROR") return 1
      return 0
    })
  }

  static levelWithSubLevels = (level: string, levels: string[]) => {
    const sortedLevels = Logs.sortLevels(levels)
    const index = sortedLevels.indexOf(level)
    return index != undefined ? sortedLevels.slice(index) : undefined
  }
}


export class Version {
  build: Array<number>

  constructor(build: Array<number>) {
    this.build = build;
  }

  static parse(version: string): Version {
    const build = version.split('.').map(Number)
    return new Version(build)
  }

  next(): Version {
    let build = new Array<number>()
    for (let i=0; i < this.build.length; i++) {
      if (i != this.build.length - 1) {
        build.push(this.build[i])
      } else {
        build.push(this.build[i] + 1)
      }
    }
    return new Version(build)
  }

  toString(): String {
    let s = new String()
    this.build.forEach(v => { if (s.length == 0) s = v.toString(); else s = s + '.' + v.toString() })
    return s
  }
}
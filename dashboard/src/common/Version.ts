import {ClientDistributionVersion, DeveloperDistributionVersion} from "../generated/graphql";

export class Version {
  build: Array<number>

  constructor(build: Array<number>) {
    this.build = build;
  }

  static parse(version: string): Version {
    const build = version.split('.').map(Number)
    return new Version(build)
  }

  static contains(clientVersion: ClientDistributionVersion, developerVersion: DeveloperDistributionVersion): boolean {
    return clientVersion.distributionName === developerVersion.distributionName &&
      clientVersion.developerBuild === developerVersion.build
  }

  static buildToString(build: Array<number>): string {
    let str = ''
    build.forEach(v => { if (str.length === 0) str += v; else str += `.${v}` })
    return str
  }

  static developerDistributionVersionToString(version: DeveloperDistributionVersion): string {
    return `${version.distributionName}-${Version.buildToString(version.build)}`
  }

  static clientDistributionVersionToString(version: ClientDistributionVersion): string {
    return `${version.distributionName}-${Version.buildToString(version.developerBuild)}_${version.clientBuild}`
  }

  next(): Version {
    let build = new Array<number>()
    for (let i=0; i < this.build.length; i++) {
      if (i !== this.build.length - 1) {
        build.push(this.build[i])
      } else {
        build.push(this.build[i] + 1)
      }
    }
    return new Version(build)
  }

  toString(): String {
    let s = ''
    this.build.forEach(v => { if (s.length === 0) s = v.toString(); else s = s + '.' + v.toString() })
    return s
  }
}
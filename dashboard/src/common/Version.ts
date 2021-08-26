import {ClientDistributionVersion, DeveloperDistributionVersion, DeveloperVersion} from "../generated/graphql";

export class Version {
  static parseBuild(version: string): Array<number> {
    return version.split('.').map(Number)
  }

  static contains(clientVersion: ClientDistributionVersion, developerVersion: DeveloperDistributionVersion): boolean {
    return clientVersion.distribution === developerVersion.distribution &&
      clientVersion.developerBuild === developerVersion.build
  }

  static buildToString(build: Array<number>): string {
    let str = ''
    build.forEach(v => { if (str.length === 0) str += v; else str += `.${v}` })
    return str
  }

  static developerVersionToString(version: DeveloperVersion): string {
    return Version.buildToString(version.build)
  }

  static developerDistributionVersionToString(version: DeveloperDistributionVersion): string {
    return `${version.distribution}-${Version.buildToString(version.build)}`
  }

  static clientDistributionVersionToString(version: ClientDistributionVersion): string {
    return (version.clientBuild === 0)?`${version.distribution}-${Version.buildToString(version.developerBuild)}`:
      `${version.distribution}-${Version.buildToString(version.developerBuild)}_${version.clientBuild}`
  }

  static compareDeveloperDistributionVersions(version1: DeveloperDistributionVersion, version2: DeveloperDistributionVersion): number {
    let result = Version.compareBuilds(version1.build, version2.build)
    if (result != 0) {
      return result
    }
    if (version1.distribution < version2.distribution) {
      return -1;
    }
    if (version1.distribution > version2.distribution) {
      return 1;
    }
    return 0;
  }

  static compareClientDistributionVersions(version1: ClientDistributionVersion, version2: ClientDistributionVersion): number {
    let result = Version.compareBuilds(version1.developerBuild, version2.developerBuild)
    if (result != 0) {
      return result
    }
    if (version1.clientBuild < version2.clientBuild) {
      return -1;
    }
    if (version1.clientBuild > version2.clientBuild) {
      return 1;
    }
    if (version1.distribution < version2.distribution) {
      return -1;
    }
    if (version1.distribution > version2.distribution) {
      return 1;
    }
    return 0;
  }

  static compareBuilds(build1: Array<number>, build2: Array<number>): number {
    let i=0
    for (; i < build1.length; i++) {
      if (build2.length <= i || build1[i] > build2[i]) {
        return 1
      }
      if (build1[i] < build2[i]) {
        return -1
      }
    }
    return (build2.length > i) ? -1 : 0
  }

  static nextBuild(build: Array<number>): Array<number> {
    let nextBuild = new Array<number>()
    for (let i=0; i < build.length; i++) {
      if (i !== build.length - 1) {
        nextBuild.push(build[i])
      } else {
        nextBuild.push(build[i] + 1)
      }
    }
    return nextBuild
  }
}
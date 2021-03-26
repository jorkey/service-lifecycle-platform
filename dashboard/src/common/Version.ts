
// @ts-ignore
class DeveloperVersion {
  build: Array<number>

  constructor(build: Array<number>) {
    this.build = build;
  }

  static parse(version: string): DeveloperVersion {
    const build = version.split('.').map(Number)
    return new DeveloperVersion(build)
  }

  next(): DeveloperVersion {
    let build = new Array<number>()
    for (let i=0; i < this.build.length; i++) {
      if (i != this.build.length - 1) {
        build.push(this.build[i])
      } else {
        build.push(this.build[i] + 1)
      }
    }
    return new DeveloperVersion(build)
  }

  toString(): String {
    let s = new String()
    this.build.forEach(v => { if (s.length == 0) s = v.toString(); else s = s + '.' + v.toString() })
    return s
  }
}

class DeveloperDistributionVersion {
  distributionName: String
  developerVersion: DeveloperVersion

  constructor(distributionName: String, developerVersion: DeveloperVersion) {
    this.distributionName = distributionName
    this.developerVersion = developerVersion
  }

  static parse(str: string): DeveloperDistributionVersion {
    const index = str.indexOf('-')
    if (index == -1) {
      throw new Error(`Developer version ${str} parse error`)
    }
    const distributionName = str.substring(0, index)
    const body = str.substring(index + 1)
    return new DeveloperDistributionVersion(distributionName, DeveloperVersion.parse(body))
  }

  toString(): String {
    return this.distributionName + '-' + this.developerVersion.toString()
  }
}

class ClientVersion {
  developerVersion: DeveloperVersion
  localBuild: number

  constructor(developerVersion: DeveloperVersion, localBuild: number = 0) {
    this.developerVersion = developerVersion
    this.localBuild = localBuild
  }

  static parse(str: string): ClientVersion {
    const index = str.indexOf('_')
    const version = (index != -1) ? str.substring(0, index) : str
    const localBuild = (index != -1) ? Number(str.substring(index + 1)) : 0
    return new ClientVersion(DeveloperVersion.parse(version), localBuild)
  }

  next(): ClientVersion {
    let localBuild = this.localBuild + 1
    return new ClientVersion(this.developerVersion, localBuild)
  }

  toString(): String {
    let str = this.developerVersion.toString()
    if (this.localBuild != 0) {
      str += (str + '_' + this.localBuild)
    }
    return str
  }
}

class ClientDistributionVersion {
  distributionName: String
  clientVersion: ClientVersion

  constructor(distributionName: String, clientVersion: ClientVersion) {
    this.distributionName = distributionName
    this.clientVersion = clientVersion
  }

  static parse(str: string): ClientDistributionVersion {
    const index = str.indexOf('-')
    if (index == -1) {
      throw new Error(`Client version ${str} parse error`)
    }
    const distributionName = str.substring(0, index)
    const body = str.substring(index + 1)
    return new ClientDistributionVersion(distributionName, ClientVersion.parse(body))
  }

  toString(): String {
    return this.distributionName + '-' + this.clientVersion.toString()
  }
}

export const Version = {
  DeveloperVersion
}

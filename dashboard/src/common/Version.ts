
class Version {
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

class ClientVersion {
  developerVersion: Version
  localBuild: number

  constructor(developerVersion: Version, localBuild: number = 0) {
    this.developerVersion = developerVersion
    this.localBuild = localBuild
  }

  static parse(str: string): ClientVersion {
    const index = str.indexOf('_')
    const version = (index != -1) ? str.substring(0, index) : str
    const localBuild = (index != -1) ? Number(str.substring(index + 1)) : 0
    return new ClientVersion(Version.parse(version), localBuild)
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

function compare(v1, v2, withLocalBuild) {
  if (v1 != null && v2 != null) {
    let [client1, build1, localBuild1] = parseVersion(v1)
    let [client2, build2, localBuild2] = parseVersion(v2)

    if (client1 != client2) {
      return (client1 > client2) ? 1 : -1
    }

    for (let i = 0; i < build1.length; ++i) {
      if (build2.length == i) {
        return 1
      }

      if (build1[i] != build2[i]) {
        return (build1[i] > build2[i]) ? 1 : -1
      }
    }

    if (withLocalBuild && localBuild1 != localBuild2) {
      return (localBuild1 > localBuild2) ? 1 : -1
    }
  }
  return 0
}

function parseVersion(version) {
  const index = version.indexOf('-')
  const client = (index != -1) ? version.substring(0, index) : null
  const body = (index != -1) ? version.substring(index + 1) : version
  const index1 = body.indexOf('_')
  const versionBody = (index1 != -1) ? body.substring(0, index1) : body
  const build = versionBody.split('.').map(Number)
  const localBuild = (index1 != -1) ? Number(body.substring(index1 + 1)) : null
  return [client, build, localBuild]
}

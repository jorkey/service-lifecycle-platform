export const Version = {
  compare
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
  const build = versionBody.split(".").map(Number)
  const localBuild = (index1 != -1) ? Number(body.substring(index1 + 1)) : null
  return [client, build, localBuild]
}

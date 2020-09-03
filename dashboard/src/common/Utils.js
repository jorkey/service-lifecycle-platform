
export const Utils = {
  login,
  logout,
  getDistributionInfo,
  getClients,
  getDesiredVersions,
  getInstalledDesiredVersions,
  getInstanceVersions,
  getServiceState
};

function login(user, password) {
  const authData = window.btoa(user + ":" + password)
  const path ="/api/user-info"
  return fetchRequest("GET", path, authData).then(user => {
    if (user) {
      user.authData = authData
      localStorage.setItem("user", JSON.stringify(user))
    }
    return user
  });
}

function logout() {
  localStorage.removeItem("user")
}

function getDistributionInfo() {
  const path ="/api/distribution-info"
  return apiGet(path);
}

function getClients() {
  const path ="/api/clients-info"
  return apiGet(path).then(clients => {
    return clients.map(client => client.name)
  });
}

function getDesiredVersions(client) {
  const path = "/api/" + (!client ? "desired-versions" : ("desired-versions/" + client))
  return apiGet(path).then(versions => {
    return versions.desiredVersions
  });
}

function getInstanceVersions(client) {
  const path = "/api/" + (!client ? "instance-versions" : ("instance-versions/" + client))
  return apiGet(path).then(versions => {
    return versions.versions
  });
}

function getServiceState(client, instanceId, directory, service) {
  const path = "/api/service-state/" + client + "/" + encodeURIComponent(instanceId) + "/" + encodeURIComponent(directory) + "/" + encodeURIComponent(service)
  return apiGet(path).then(state => {
    return state
  });
}

function getInstalledDesiredVersions(client) {
  return apiGet(["installed-desired-versions", client]).then(versions => {
    return versions.desiredVersions
  });
}

function apiGet(path) {
  let pathStr = ""
  path.foreach (p => { pathStr += "/" + encodeURIComponent(pathStr) })
  console.log(`Get ${pathStr}`)
  const user = localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")): undefined
  return fetchRequest("GET", pathStr, user ? user.authData: undefined).then(
    data => { return data },
    response => {
      if (response.status === 401) {
        logout()
        // eslint-disable-next-line no-restricted-globals
        location.reload(true)
      } else {
        return Promise.reject(response)
      }
    })
}

function fetchRequest(method, path, authData) {
  console.log(`Fetch ${method} ${path}`)
  const requestInit = {}
  requestInit.method = method
  const headers = { "Content-Type": "application/json" }
  if (authData) {
    headers.Authorization = "Basic " + authData
  }
  requestInit.headers = headers
  requestInit.cache = "no-cache"
  return fetch(path, requestInit).then(response => {
    console.log("handleResponse")
    if (response.ok) {
      return response.text().then(text => {
          console.log("Response: " + text)
          return text && JSON.parse(text)
        }
      )
    } else {
      console.log("Response error: " +  response.statusText)
      return Promise.reject(response)
    }
  })
}

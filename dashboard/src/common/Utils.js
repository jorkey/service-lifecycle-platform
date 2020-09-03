
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
  return apiGet("distribution-info");
}

function getClients() {
  return apiGet("clients-info").then(clients => {
    return clients.map(client => client.name)
  });
}

function getDesiredVersions(client) {
  return apiGet("desired-versions", client).then(versions => {
    return versions.desiredVersions
  });
}

function getInstanceVersions(client) {
  return apiGet("instance-versions", client).then(versions => {
    return versions.versions
  });
}

function getServiceState(client, instanceId, directory, service) {
  return apiGet( "service-state", instanceId, directory, service).then(state => {
    return state
  });
}

function getInstalledDesiredVersions(client) {
  return apiGet("installed-desired-versions", client).then(versions => {
    return versions.desiredVersions
  });
}

function apiGet(p1, p2, p3, p4, p5, p6, p7, p8) {
  let pathStr = "/api"
  if (p1) { pathStr += "/" + encodeURIComponent(p1) }
  if (p2) { pathStr += "/" + encodeURIComponent(p2) }
  if (p3) { pathStr += "/" + encodeURIComponent(p3) }
  if (p4) { pathStr += "/" + encodeURIComponent(p4) }
  if (p5) { pathStr += "/" + encodeURIComponent(p5) }
  if (p6) { pathStr += "/" + encodeURIComponent(p6) }
  if (p7) { pathStr += "/" + encodeURIComponent(p7) }
  if (p8) { pathStr += "/" + encodeURIComponent(p8) }
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

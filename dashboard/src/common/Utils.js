
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
  const authData = window.btoa(user + ':' + password)
  const path ='/api/user-info'
  const init = {}
  init.method = 'GET'
  const headers = { 'Content-Type': 'application/json' }
  headers.Authorization = 'Basic ' + authData
  init.headers = headers
  init.cache = 'no-cache'
  return fetchRequest(path, init).then(user => {
    if (user) {
      user.authData = authData
      localStorage.setItem('user', JSON.stringify(user))
    }
    return user
  });
}

function logout() {
  localStorage.removeItem('user')
}

function getDistributionInfo() {
  return apiGet('distribution-info');
}

function getClients() {
  return apiGet('clients-info').then(clients => {
    return clients.map(client => client.name)
  });
}

function getDesiredVersions(client) {
  return apiGet('desired-versions', client).then(versions => {
    return versions.desiredVersions
  });
}

function getInstanceVersions(client) {
  return apiGet('instance-versions', client).then(versions => {
    return versions.versions
  });
}

function getServiceState(client, instanceId, directory, service) {
  return apiGet( 'service-state', client, instanceId, directory, service).then(state => {
    return state
  });
}

function getInstalledDesiredVersions(client) {
  return apiGet('installed-desired-versions', client).then(versions => {
    return versions.desiredVersions
  });
}

function fetchRequest(path, init) {
  console.log(`Fetch ${path} ${init}`)
  return fetch(path, init).then(response => {
    console.log('handleResponse')
    if (response.ok) {
      return response.text().then(text => {
        console.log('Response: ' + text)
        return text && JSON.parse(text)
      })
    } else {
      console.log('Response error: ' +  response.statusText)
      return Promise.reject(response)
    }
  })
}

function apiGet(p1, p2, p3, p4, p5, p6, p7, p8) {
  let path = '/api'
  if (p1) { path += '/' + encodeURIComponent(p1) }
  if (p2) { path += '/' + encodeURIComponent(p2) }
  if (p3) { path += '/' + encodeURIComponent(p3) }
  if (p4) { path += '/' + encodeURIComponent(p4) }
  if (p5) { path += '/' + encodeURIComponent(p5) }
  if (p6) { path += '/' + encodeURIComponent(p6) }
  if (p7) { path += '/' + encodeURIComponent(p7) }
  if (p8) { path += '/' + encodeURIComponent(p8) }
  console.log(`Get ${path}`)
  const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')): undefined
  const authData = user ? user.authData: undefined
  const init = {}
  init.method = 'GET'
  const headers = { 'Content-Type': 'application/json' }
  if (authData) {
    headers.Authorization = 'Basic ' + authData
  }
  init.headers = headers
  init.cache = 'no-cache'
  return fetchRequest(path, init).then(
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

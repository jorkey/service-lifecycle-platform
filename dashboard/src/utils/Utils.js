
export const Utils = {
  login,
  logout,
  getClients,
  getDesiredVersions,
  getInstanceVersions
};

function login(user, password) {
  let authData = window.btoa(user + ':' + password)
  let path ='/api/user-info'
  return fetchRequest('GET', path, authData).then(user => {
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

function getClients() {
  let path ='/api/clients-info'
  return get(path).then(clients => {
    return clients.map(client => client.name)
  });
}

function getDesiredVersions(client) {
  let path = '/api/' + (!client ? 'desired-versions' : ('desired-versions/' + client))
  return get(path).then(versions => {
    return versions.desiredVersions
  });
}

function getInstanceVersions(client) {
  let path = '/api/instance-versions/' + client
  return get(path).then(versions => {
    console.log("--- versions " + JSON.stringify(versions))
    console.log("--- versions.versions " + versions.versions)
    return versions.versions
  });
}

function get(path) {
  console.log(`Get ${path}`)
  let user = JSON.parse(localStorage.getItem('user'))
  return fetchRequest('GET', path, user.authData).then(
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
  let requestInit = {}
  requestInit.method = method
  let headers = { 'Content-Type': 'application/json' }
  if (authData) {
    headers.Authorization = 'Basic ' + authData
  }
  requestInit.headers = headers
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

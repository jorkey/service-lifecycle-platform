
export const Utils = {
  login,
  logout,
  get
};

function login(user, password) {
  let authData = window.btoa(user + ':' + password)
  return fetchRequest('GET', '/login', authData).then(user => {
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

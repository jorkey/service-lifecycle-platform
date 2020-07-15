// TODO import config from 'config';

export const Utils = {
    login,
    logout,
    get
};

function login(user, password) {
    return get('/login', user, password).then(user => {
        if (user) {
            user.authdata = window.btoa(user + ':' + password)
            localStorage.setItem('user', JSON.stringify(user))
        }
        return user
      });
}

function logout() {
    localStorage.removeItem('user')
}

function get(path, user, password) {
    console.log("get")
    // TODO return fetch(`${config.apiUrl}/${url}`, {
    return fetch(`${path}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Basic ' + window.btoa(user + ':' + password)
            }
        }).then(handleResponse);
}

function handleResponse(response) {
    console.log("handleResponse")
    if (response.ok) {
        const text = response.text
        console.log("Response: " +  response.status)
        return text && JSON.parse(text)
    } else {
        logout()
        console.log("Response error: " +  response.statusText)
        // eslint-disable-next-line no-restricted-globals
        //location.reload(true)
        return Promise.reject(response.status + ": " + response.statusText)
    }
}

export function authHeader1() {
    let user = JSON.parse(localStorage.getItem('user'))

    if (user && user.authdata) {
        return { 'Authorization': 'Basic ' + user.authdata }
    } else {
        return {}
    }
}

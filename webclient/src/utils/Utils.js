import config from 'config';

export const Utils = {
    login,
    logout,
    get
};

function login(username, password) {
    return fetch(`${config.apiUrl}/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    }).then(handleResponse)
      .then(user => {
        if (user) {
            user.authdata = window.btoa(username + ':' + password)
            localStorage.setItem('user', JSON.stringify(user))
        }
        return user
      });
}

function logout() {
    localStorage.removeItem('user')
}

function get(url) {
    return fetch(`${config.apiUrl}/${url}`, {
            method: 'GET',
            headers: authHeader()
        }).then(handleResponse);
}

function handleResponse(response) {
    if (response.ok) {
        const text = response.text
        return text && JSON.parse(text)
    } else {
        logout()
        console.log("Response error: " +  response.statusText)
        // eslint-disable-next-line no-restricted-globals
        //location.reload(true)
        return Promise.reject(response.status + ": " + response.statusText)
    }
}

export function authHeader() {
    let user = JSON.parse(localStorage.getItem('user'))

    if (user && user.authdata) {
        return { 'Authorization': 'Basic ' + user.authdata }
    } else {
        return {}
    }
}

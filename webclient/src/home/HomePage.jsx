import React from 'react';

import { Utils } from '../utils';

class HomePage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            user: {},
        };
    }

    componentDidMount() {
        this.setState({
            user: JSON.parse(localStorage.getItem('user')),
            users: { loading: true }
        });
        //Utils.getAll().then(users => this.setState({ users }));
    }

    render() {
        const { user } = this.state;
        return (
            <div>Home page</div>
        );
    }
}

export { HomePage };

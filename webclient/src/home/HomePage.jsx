import React from 'react';
import {Utils} from "../utils";

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
        });
        Utils.get("/sdsds")
    }

    render() {
        //const { user } = this.state;
        return (
            <div>Home page</div>
        );
    }
}

export { HomePage };

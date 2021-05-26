import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import ProfilesManager from "./components/Profiles/ProfilesManager";
import ProfileEditor from "./components/Profiles/ProfileEditor";
import {Route, Switch, useRouteMatch} from "react-router-dom";
import SourcesEditor from "./components/Development/SourcesEditor";
import ServicesManager from "./components/Development/ServicesManager";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Services = () => {
  const classes = useStyles();
  const routeMatch = useRouteMatch();

  return (
    <div className={classes.root}>
      <Grid
        container
      >
        <Grid
          item
          xs={12}
        >
          <Switch>
            <Route exact path={`${routeMatch.url}/development`}
                   component={ServicesManager}/>
            <Route exact path={`${routeMatch.url}/development/new`}
                   render={(props) => <SourcesEditor fromUrl={`${routeMatch.url}/development`} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/development/edit/:service`}
                   render={(props) => <SourcesEditor fromUrl={`${routeMatch.url}/development`} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/profiles`}
              component={ProfilesManager}/>
            <Route exact path={`${routeMatch.url}/profiles/new`}
              render={(props) => <ProfileEditor fromUrl={`${routeMatch.url}/profiles`} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/profiles/edit/:profile`}
              render={(props) => <ProfileEditor fromUrl={`${routeMatch.url}/profiles`} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Services;

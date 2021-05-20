import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import ProfilesManager from "./components/Profiles/ProfilesManager";
import ProfileEditor from "./components/Profiles/ProfileEditor";
import {Route, Switch, useRouteMatch} from "react-router-dom";
import SourcesEditor from "./components/Sources/SourcesEditor";
import SourcesManager from "./components/Sources/SourcesManager";

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
            <Route exact path={`${routeMatch.url}/sources`}
                   component={SourcesManager}/>
            <Route exact path={`${routeMatch.url}/sources/new`}
                   render={(props) => <SourcesEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/sources/edit/:source`}
                   render={(props) => <SourcesEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/profiles`}
              component={ProfilesManager}/>
            <Route exact path={`${routeMatch.url}/profiles/new`}
              render={(props) => <ProfileEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/profiles/edit/:profile`}
              render={(props) => <ProfileEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Services;

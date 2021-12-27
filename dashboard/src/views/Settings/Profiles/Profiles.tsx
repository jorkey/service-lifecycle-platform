import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import ProfilesManager from "./components/ProfilesManager";
import ProfileEditor from "./components/ProfileEditor";
import ProvidersManager from "../Providers/components/Providers/ProvidersManager";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Profiles = () => {
  const classes = useStyles();
  const routeMatch = useRouteMatch();

  console.log('profiles route 2 ' + routeMatch.url)

  return (<ProfilesManager/>);
  // return (
  //   <div className={classes.root}>
  //     <Grid
  //       container
  //     >
  //       <Grid
  //         item
  //         xs={12}
  //       >
  //         <Switch>
  //           <Route exact path={`${routeMatch.url}/profiles`}
  //                  component={ProvidersManager}/>
  //           <Route exact path={`${routeMatch.url}/profiles/new`}
  //                  render={(props) => <ProfileEditor fromUrl={`${routeMatch.url}/profiles`} {...props} /> }>
  //           </Route>
  //           <Route exact path={`${routeMatch.url}/profiles/edit/:profile`}
  //                  render={(props) => <ProfileEditor fromUrl={`${routeMatch.url}/profiles`} {...props} /> }>
  //           </Route>
  //         </Switch>
  //       </Grid>
  //     </Grid>
  //   </div>
  // );
};

export default Profiles;

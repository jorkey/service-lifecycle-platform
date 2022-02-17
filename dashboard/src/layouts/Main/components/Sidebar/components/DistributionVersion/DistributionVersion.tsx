import React from 'react';
import { makeStyles } from '@material-ui/styles';
import { Typography } from '@material-ui/core';
import {
  useClientDesiredVersionQuery,
} from "../../../../../../generated/graphql";
import {Version} from "../../../../../../common";

const useStyles = makeStyles(theme => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    minHeight: 'fit-content'
  },
  version: {
  }
}));

const DistributionVersion = () => {
  const classes = useStyles()

  const { data:info } = useClientDesiredVersionQuery({
    variables: { service: 'distribution' },
  })

  const currentVersion = Version.parseClientDistributionVersion(localStorage.getItem('distributionVersion')!)

  return info?.clientDesiredVersions.length ?
    <div className={classes.root}>
      <Typography
        className={classes.version}
        variant='body2'
      >
        Server Version {Version.clientDistributionVersionToString(currentVersion)}
      </Typography>
    </div> : null
};

export default DistributionVersion;

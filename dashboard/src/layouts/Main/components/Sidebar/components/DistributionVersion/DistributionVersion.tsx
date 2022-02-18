import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {Button, InputLabel, Typography} from '@material-ui/core';
import {
  useClientDesiredVersionQuery,
} from "../../../../../../generated/graphql";
import {Version} from "../../../../../../common";
import RefreshIcon from "@material-ui/icons/Refresh";

const useStyles = makeStyles(theme => ({
  root: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 'fit-content'
  },
}));

const DistributionVersion = () => {
  const classes = useStyles()

  const [ clientVersion ] = useState(Version.parseClientDistributionVersion(localStorage.getItem('distributionVersion')!))
  const [ timeout ] = useState<any[1]>([undefined])

  const { data:version, refetch:getVersion } = useClientDesiredVersionQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { service: 'distribution' },
  })

  const scheduleGetVersion = () => {
    if (!timeout[0]) {
      timeout[0] = setInterval(() => {
        getVersion()
      }, 15000)
    }
  }

  useEffect(() => { scheduleGetVersion() }, [])

  document.addEventListener("visibilitychange", (e) => {
    const visible = document.visibilityState == 'visible'
    if (visible) {
      scheduleGetVersion()
    } else if (timeout[0]) {
      clearInterval(timeout[0])
      timeout[0] = undefined
    }
  })

  const serverVersion = version?.clientDesiredVersions[0].version
  const upToDate = serverVersion?Version.compareClientDistributionVersions(clientVersion, serverVersion)==0:true

  return <div className={classes.root}>
    {upToDate?<Typography
      style={{paddingLeft: '10px'}}
      variant='body2'
    >
      Server Version: {Version.clientDistributionVersionToString(clientVersion)}
    </Typography>:
      <Button
        style={{textTransform: 'none'}}
        href='/'
        title='Refresh Page'
      >
        <InputLabel style={{color: 'red'}}>
          Refresh Page To Version {Version.clientDistributionVersionToString(clientVersion)}
        </InputLabel>
        <RefreshIcon style={{color: 'red'}}/>
      </Button>}
  </div>
};

export default DistributionVersion;

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
    flexDirection: 'column',
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
      getVersion()
      scheduleGetVersion()
    } else if (timeout[0]) {
      clearInterval(timeout[0])
      timeout[0] = undefined
    }
  })

  const serverVersion = version?.clientDesiredVersions[0].version
  const upToDate = serverVersion?
    clientVersion.distribution == serverVersion.distribution &&
    Version.compareBuilds(clientVersion.developerBuild, serverVersion.developerBuild) == 0:true

  return <div className={classes.root}>
    {upToDate?<Typography
      style={{paddingLeft: '10px', textAlign: 'center'}}
      variant='body2'
    >
      Server Version: {Version.clientDistributionVersionToString(clientVersion)}
    </Typography>:
      <Button
        href='/'
        title='Refresh Page'
      >
        <div style={{display: 'flex', flexDirection: 'column', paddingRight: '10px' }}>
          <InputLabel style={{color: 'red', paddingBottom: '4px'}}>
            Refresh page to version
          </InputLabel>
          <InputLabel style={{color: 'red', fontStyle: 'italic'}}>
            {Version.clientDistributionVersionToString(serverVersion!)}
          </InputLabel>
        </div>
        <RefreshIcon style={{color: 'red'}}/>
      </Button>}
  </div>
};

export default DistributionVersion;

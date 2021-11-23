import React, {useEffect, useState} from "react";
import {Grid, IconButton, Typography} from "@material-ui/core";
import BackIcon from '@mui/icons-material/ArrowBack';
import ForwardIcon from '@mui/icons-material/ArrowForward';
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles((theme:any) => ({
  root: {
  },
  timeLeft: {
    maxWidth: 175,
    minWidth: 175,
  },
  timeLeftText: {
    textAlign: 'right',
    paddingTop: 2
  },
  time: {
    maxWidth: 175,
    minWidth: 175,
  },
  timeText: {
    textAlign: 'center',
    paddingTop: 2
  },
  timeRight: {
    maxWidth: 175,
    minWidth: 175,
  },
  timeRightText: {
    textAlign: 'left',
    paddingTop: 2
  },
  button: {
    maxWidth: 30,
    padding: 0
  }
}))

interface TimeSelectorParams {
  time: Date | undefined
  times: Date[]
  onSelected: (time: Date) => void
}

const TimeSelector: React.FC<TimeSelectorParams> = props => {
  const { time, times, onSelected } = props

  const [timeLeft, setTimeLeft] = useState<Date>()
  const [timeRight, setTimeRight] = useState<Date>()

  const classes = useStyles()

  useEffect(() => {
    let left: Date | undefined = undefined
    let right: Date | undefined = undefined
    times.forEach(t => {
      if (time) {
        if (t < time) {
          if (left == undefined || left < t) {
            left = t
          }
        } else if (t > time) {
          if (right == undefined || right > t) {
            right = t
          }
        }
      }
    })
    setTimeLeft(left)
    setTimeRight(right)
  }, [ time ])

  if (!time && times.length) {
    onSelected(times[times.length-1])
  }

  return time?
    <Grid container direction={'row'} spacing={1} className={classes.root}>
      <Grid item md={3} xs={11} className={classes.timeLeft}>
        <Typography className={classes.timeLeftText}>{timeLeft?.toLocaleString()}</Typography>
      </Grid>
      <Grid item md={1} xs={12} className={classes.button}>
        <IconButton
          className={classes.button}
          disabled={!timeLeft}
          onClick={() => { if (timeLeft) onSelected(timeLeft) }}>
          <BackIcon className={classes.button}/>
        </IconButton>
      </Grid>
      <Grid item md={3} xs={11} className={classes.time}>
        <Typography className={classes.timeText}>{time.toLocaleString()}</Typography>
      </Grid>
      <Grid item md={1} xs={11} className={classes.button}>
        <IconButton
          className={classes.button}
          disabled={!timeRight}
          onClick={() => { if (timeRight) onSelected(timeRight) }}>
          <ForwardIcon className={classes.button}/>
        </IconButton>
      </Grid>
      <Grid item md={3} xs={11} className={classes.timeRight}>
        <Typography className={classes.timeRightText}>{timeRight?.toLocaleString()}</Typography>
      </Grid>
    </Grid> : null
}

export default TimeSelector
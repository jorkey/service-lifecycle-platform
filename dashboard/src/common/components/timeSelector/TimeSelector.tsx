import React, {useEffect, useState} from "react";
import {Grid, IconButton, Typography} from "@material-ui/core";
import LeftIcon from '@mui/icons-material/ArrowLeft';
import RightIcon from '@mui/icons-material/ArrowRight';

interface TimeSelectorParams {
  time: Date
  times: Date[]
  selected: (time: Date) => void
  confirmed: () => void
  canceled: () => void
}

const TimeSelector: React.FC<TimeSelectorParams> = props => {
  const { time, times, selected, confirmed, canceled } = props

  const [timeLeft, setTimeLeft] = useState<Date>()
  const [timeRight, setTimeRight] = useState<Date>()

  useEffect(() => {
      let left: Date | undefined = undefined
      let right: Date | undefined = undefined
      times.forEach(t => {
        if (t < time) {
          if (left == undefined || left < t) {
            left = t
          }
        } else if (t > time) {
          if (right == undefined || right > t) {
            right = t
          }
        }
      })
      setTimeLeft(left)
      setTimeRight(right)
    }, [ time ])

  return (
    <Grid container direction={'row'} spacing={1}>
      <Grid item md={3} xs={11}>
        <Typography>{timeLeft?.toLocaleString()}</Typography>
      </Grid>
      <Grid item md={1} xs={12}>
        <IconButton
          disabled={!timeLeft}
          onClick={() => { if (timeLeft) selected(timeLeft) }}>
          <LeftIcon/>
        </IconButton>
      </Grid>
      <Grid item md={3} xs={11}>
        <Typography>{time?.toLocaleString()}</Typography>
      </Grid>
      <Grid item md={1} xs={11}>
        <IconButton
          disabled={!timeRight}
          onClick={() => { if (timeRight) selected(timeRight) }}>
          <RightIcon/>
        </IconButton>
      </Grid>
      <Grid item md={3} xs={11}>
        <Typography>{timeRight?.toLocaleString()}</Typography>
      </Grid>
    </Grid>
  );
}

export default TimeSelector
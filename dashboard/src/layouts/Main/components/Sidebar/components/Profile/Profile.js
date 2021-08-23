import React from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import { Typography } from '@material-ui/core';
import {useWhoAmIQuery} from "../../../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  root: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    minHeight: 'fit-content'
  },
  avatar: {
    width: 60,
    height: 60
  },
  name: {
    marginTop: theme.spacing(1)
  }
}));

const Profile = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const { data } = useWhoAmIQuery();

  return data ?
    <div
      {...rest}
      className={clsx(classes.root, className)}
    >
      <Typography
        className={classes.name}
        variant='h4'
      >
        {data.whoAmI.name}
      </Typography>
      <Typography variant='body2'>{data.whoAmI.role.toString()}</Typography>
    </div> : null
};

Profile.propTypes = {
  className: PropTypes.string
};

export default Profile;

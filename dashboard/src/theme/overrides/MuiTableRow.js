import palette from '../palette';

export default {
  root: {
    '&$selected': {
      backgroundColor: palette.background.default
    },
    '&:hover': {
      backgroundColor: palette.text.secondary
    }
  }
};

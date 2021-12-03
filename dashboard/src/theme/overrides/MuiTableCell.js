import palette from '../palette';
import typography from '../typography';

export default {
  root: {
    ...typography.body1,
    padding: '0px',
    paddingLeft: '16px',
    borderBottom: `1px solid ${palette.divider}`
  }
};

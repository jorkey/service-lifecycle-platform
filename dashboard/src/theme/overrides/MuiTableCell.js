import palette from '../palette';
import typography from '../typography';

export default {
  root: {
    ...typography.body1,
    padding: '2px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderBottom: `1px solid ${palette.divider}`,
  }
};

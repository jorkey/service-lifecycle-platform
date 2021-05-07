import React, {useState} from "react";
import {IconButton, Input, Table, TableBody, TableCell, TableHead, TableRow} from "@material-ui/core";
import DoneIcon from "@material-ui/icons/DoneAllTwoTone";
import RevertIcon from "@material-ui/icons/NotInterestedOutlined";
import EditIcon from "@material-ui/icons/Edit";
import ArrowForwardIcon from "@material-ui/icons/ArrowForward";
import DeleteIcon from "@material-ui/icons/Delete";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  serviceColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px'
  }
}));

export enum ServiceProfileType {
  Alone,
  Pattern,
  Projection
}

interface Row {
  id: number,
  service: string
  isEditMode: boolean
}

interface CustomTableCellParams {
  row: Row
  classes: any
  onChange: any
}

const CustomTableRow = (params: CustomTableCellParams) => {
  const { row, onChange } = params
  const classes = useStyles();
  return (
    <TableCell align="left" className={classes.serviceColumn}>
      {row.isEditMode ? (
        <Input
          value={row.service}
          onChange={e => onChange(e, row)}
        />
      ) : (
        row.service
      )}
    </TableCell>
  )
}

interface ServicesTableParams {
  profileType: ServiceProfileType
  getServices?: () => Array<string>
  setServices?: (services: Array<string>) => any
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { profileType, getServices: getServices, setServices } = props;
  const [rows, setRows] = useState(getServices?.().map((service, index) => {
    return { id: index, service: service, isEditMode: false };
  }));
  const [previous, setPrevious] = useState<Row>();
  const classes = useStyles();

  const onToggleEditMode = (id: number) => {
    setRows(state => {
      return rows?.map(row => {
        if (row.id === id) {
          return { ...row, isEditMode: !row.isEditMode };
        }
        return row;
      });
    });
  };

  const onChange = (e:any, row:Row) => {
    // if (!previous[row.id]) {
    //   setPrevious(state => ({ ...state, [row.id]: row }));
    // }
    const value = e.target.value;
    const name = e.target.name;
    const { id } = row;
    const newRows = rows?.map(row => {
      if (row.id === id) {
        return { ...row, service: value };
      }
      return row;
    });
    setRows(newRows);
  };

  // const onRevert = (id:any) => {
  //   const newRows = rows?.map(row => {
  //     if (row.id === id) {
  //       return previous[id] ? previous[id] : row;
  //     }
  //     return row;
  //   });
  //   setRows(newRows);
  //   setPrevious(state => {
  //     delete state[id];
  //     return state;
  //   });
  //   onToggleEditMode(id);
  // };

  return (
    <Table
      className={classes.servicesTable}
      stickyHeader
    >
      <TableHead>
        <TableRow>
          <TableCell className={classes.serviceColumn}>Service</TableCell>
          <TableCell className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      { rows ?
        <TableBody>
          {rows.map(row =>
            (<TableRow
              hover
              key={row.id}
            >
              <CustomTableRow key={row.id} row={row} classes={classes} onChange={onChange}/>
              <TableCell className={classes.actionsColumn}>
                {row.isEditMode ? (
                  <>
                    <IconButton
                      aria-label="done"
                      onClick={() => onToggleEditMode(row.id)}
                    >
                      <DoneIcon />
                    </IconButton>
                    <IconButton
                      aria-label="revert"
                      // onClick={() => onRevert(row.id)}
                    >
                      <RevertIcon />
                    </IconButton>
                  </>
                ) : (<>
                  <IconButton
                    onClick={() => onToggleEditMode(row.id)}
                    title="Edit"
                  >
                    <EditIcon/>
                  </IconButton>
                  <IconButton
                    onClick={() => setRows(rows.filter(r => r.id != row.id) )}
                    title="Delete"
                  >
                    { profileType == ServiceProfileType.Projection ? <ArrowForwardIcon/> : <DeleteIcon/> }
                  </IconButton>
                </>)}
              </TableCell>
            </TableRow>)
          )}
        </TableBody> : null }
    </Table>)
}

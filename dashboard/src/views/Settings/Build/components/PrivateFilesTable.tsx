import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import DeleteIcon from "@material-ui/icons/Delete";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";

const useStyles = makeStyles(theme => ({
  repositoriesTable: {
    marginTop: 20
  },
  fileColumn: {
    width: '200px',
  },
  uploadColumn: {
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
}));

interface PrivateFilesParams {
  privateFiles: Array<string>
  addPrivateFile?: boolean
  confirmRemove?: boolean
  onPrivateFileAdded?: (file: string, localFile: string) => void
  onPrivateFileAddCancelled?: () => void
  onPrivateFileRemoved?: (file: string) => void
}

const PrivateFilesTable = (props: PrivateFilesParams) => {
  const { privateFiles, addPrivateFile, confirmRemove,
    onPrivateFileAdded, onPrivateFileAddCancelled, onPrivateFileRemoved } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'file',
      headerName: 'File',
      className: classes.fileColumn,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('name')?.value == value
          })
      }
    },
    {
      name: 'upload',
      headerName: 'Upload',
      className: classes.uploadColumn,
      // validate: (value, rowNum) => {
      //   try {
      //     return value?!!new URL(value as string):false
      //   } catch (ex) {
      //     return false
      //   }
      // }
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = privateFiles.map(file => (
    new Map<string, GridTableCellParams>([
      ['name', { value: file }],
      ['actions', { value: [<Button key='0' onClick={
          () => confirmRemove ? setDeleteConfirm(file) : onPrivateFileRemoved?.(file) }>
        <DeleteIcon/>
      </Button>] }]
    ])))

  return (<>
    <GridTable
      className={classes.repositoriesTable}
      columns={columns}
      rows={rows}
      addNewRow={addPrivateFile}
      onRowAdded={ (columns) =>
        new Promise<boolean>(resolve => {
          onPrivateFileAdded?.(columns.get('name')! as string, columns.get('upload')! as string)
          resolve(true)
        })}
      onRowAddCancelled={onPrivateFileAddCancelled}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete file '${deleteConfirm}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onPrivateFileRemoved?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}

export default PrivateFilesTable;

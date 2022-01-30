import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import DeleteIcon from "@material-ui/icons/Delete";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import {FileInfo} from "../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  repositoriesTable: {
    marginTop: 20
  },
  pathColumn: {
    width: '300px'
  },
  timeColumn: {
    width: '300px'
  },
  lengthColumn: {
    width: '300px'
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
  privateFiles: Array<{info:FileInfo, localFile:File|null}>
  addPrivateFile?: boolean
  confirmRemove?: boolean
  onPrivateFileAdded?: (path: string, localFile: File) => void
  onPrivateFileAddCancelled?: () => void
  onPrivateFileRemoved?: (path: string) => void
}

const PrivateFilesTable = (props: PrivateFilesParams) => {
  const { privateFiles, addPrivateFile, confirmRemove,
    onPrivateFileAdded, onPrivateFileAddCancelled, onPrivateFileRemoved } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()
  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'path',
      headerName: 'Path',
      className: classes.pathColumn,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('name')?.value == value
          })
      }
    },
    {
      name: 'time',
      type: 'date',
      editable: false,
      headerName: 'Time',
      className: classes.timeColumn
    },
    {
      name: 'length',
      type: 'number',
      editable: false,
      headerName: 'Length',
      className: classes.lengthColumn
    },
    {
      name: 'upload',
      headerName: 'File To Upload',
      className: classes.uploadColumn,
      type: 'upload',
      validate: (value, rowNum) => {
        return !!value
      }
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = privateFiles
    .sort((f1, f2) =>
      f1.info > f2.info ? 1 : f1.info < f2.info ? -1 : 0)
    .map(file => (
      new Map<string, GridTableCellParams>([
        ['path', { value: file.info.path }],
        ['time', { value: file.info.time }],
        ['length', { value: file.info.length }],
        ['upload', { value: file.localFile?file.localFile:'' }],
        ['actions', { value: [<Button key='0' onClick={
            () => confirmRemove ? setDeleteConfirm(file.info.path) : onPrivateFileRemoved?.(file.info.path) }>
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
          onPrivateFileAdded?.(columns.get('path')! as string, columns.get('upload')! as File)
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

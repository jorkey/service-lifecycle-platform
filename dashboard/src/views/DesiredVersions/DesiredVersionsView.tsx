import {Box, Button, Card, CardContent, CardHeader, FormControlLabel} from "@material-ui/core";
import FormGroup from "@material-ui/core/FormGroup";
import TimeSelector from "./TimeSelector";
import {RefreshControl} from "../../common/components/refreshControl/RefreshControl";
import GridTable from "../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import React from "react";
import {GridTableCellParams, GridTableColumnParams} from "../../common/components/gridTable/GridTableColumn";

export class DesiredVersionWrap<DesiredVersion> {
  desiredVersion: DesiredVersion
  service: string
  version: string
  author: string
  buildTime: string
  comment: string

  constructor(desiredVersion: DesiredVersion,
              service: string, version: string, author: string, buildTime: string, comment: string) {
    this.desiredVersion = desiredVersion
    this.service = service
    this.version = version
    this.author = author
    this.buildTime = buildTime
    this.comment = comment
  }
}

type Classes = Record<"root" | "content" | "inner" | "versionsTable" |
                      "serviceColumn" | "versionColumn" | "boldVersionColumn" | "authorColumn" | "timeColumn" | "commentColumn" |
                      "controls" | "control" | "alert" | "historyButton", string>

export class DesiredVersionsView<DesiredVersion> {
  private title: string
  private wrap: (version: DesiredVersion) => DesiredVersionWrap<DesiredVersion>
  private compare: (v1: DesiredVersionWrap<DesiredVersion> | undefined,
                    v2: DesiredVersionWrap<DesiredVersion> | undefined) => number
  private modifyDesiredVersions: (desiredVersions: DesiredVersion[]) => void
  private rerender: () => void
  private refresh: () => void
  private classes: Classes

  private desiredVersions: DesiredVersionWrap<DesiredVersion>[] = []
  private originalDesiredVersions: DesiredVersionWrap<DesiredVersion>[] = []
  private desiredVersionsHistory: {time:Date, versions:DesiredVersionWrap<DesiredVersion>[]}[] = []

  private columns: Array<GridTableColumnParams> = []
  private rows: Map<string, GridTableCellParams>[] = []
  private timeSelect: Date | undefined = undefined
  private error: string | undefined = undefined

  constructor(title: string,
              wrap: (version: DesiredVersion) => DesiredVersionWrap<DesiredVersion>,
              compare: (v1: DesiredVersionWrap<DesiredVersion> | undefined, v2: DesiredVersionWrap<DesiredVersion> | undefined) => number,
              modifyDesiredVersions: (desiredVersions: DesiredVersion[]) => void,
              rerender: () => void, refresh: () => void, classes: Classes) {
    this.title = title
    this.wrap = wrap
    this.compare = compare
    this.modifyDesiredVersions = modifyDesiredVersions
    this.rerender = rerender
    this.refresh = refresh
    this.classes = classes
  }

  setDesiredVersions(desiredVersions: DesiredVersion[]) {
    this.desiredVersions = desiredVersions.map(version => this.wrap(version))
  }

  setDesiredVersionsHistory(desiredVersionsHistory: {time:Date, versions:DesiredVersion[]}[]) {
    this.desiredVersionsHistory =
      desiredVersionsHistory.map(v => { return {time: v.time, versions: v.versions.map(v => this.wrap(v))}})
  }

  setError(error: string | undefined) {
    this.error = error
  }

  getColumns() {
    return [
      {
        name: 'service',
        headerName: 'Service',
        className: this.classes.serviceColumn,
      },
      {
        name: 'version',
        type: 'select',
        headerName: 'Desired Version',
        className: this.classes.versionColumn,
      },
      {
        name: 'author',
        headerName: 'Author',
        className: this.classes.authorColumn,
      },
      {
        name: 'buildTime',
        headerName: 'Build Time',
        type: 'date',
        className: this.classes.timeColumn,
      },
      {
        name: 'comment',
        headerName: 'Comment',
        className: this.classes.commentColumn,
      }
    ] as Array<GridTableColumnParams>
  }

  setColumns(columns: Array<GridTableColumnParams>) {
    this.columns = columns
  }

  getRows() {
    return this.makeServicesList().map(service => {
      const currentVersion = this.desiredVersions.find(v => v.service == service)
      const originalVersion = this.originalDesiredVersions.find(v => v.service == service)
      const version = currentVersion ? currentVersion : originalVersion!
      const modified = this.compare(currentVersion, originalVersion)
      return new Map<string, GridTableCellParams>([
        ['service', { value: service }],
        ['version', {
          value: version.version,
          className: modified?this.classes.boldVersionColumn:undefined
        }],
        ['author', { value: version.author }],
        ['buildTime', { value: version.buildTime }],
        ['comment', { value: version.comment }]
      ])})
  }

  setRows(rows: Map<string, GridTableCellParams>[]) {
    this.rows = rows
  }

  private makeServicesList() {
    const services = new Set<string>()
    this.originalDesiredVersions.map(v => v.service).forEach(s => services.add(s))
    this.desiredVersions.map(v => v.service).forEach(s => services.add(s))
    return Array.from(services)
  }

  render() {
    return (
      <Card
        className={this.classes.root}
      >
        <CardHeader
          action={
            <FormGroup row>
              <FormControlLabel
                label={null}
                control={
                  this.timeSelect ?
                    <TimeSelector time={this.timeSelect}
                                  times={this.desiredVersionsHistory.map(v => v.time).sort()}
                                  onSelected={(t) => {
                                    this.timeSelect = t
                                    const versions = this.desiredVersionsHistory.find(v => v.time == this.timeSelect)?.versions
                                    if (versions) {
                                      this.modifyDesiredVersions(versions.map(v => v.desiredVersion))
                                    }
                                  }}
                    /> :
                    <Button
                      className={this.classes.historyButton}
                      color="primary"
                      variant="contained"
                      onClick={() => {
                        const history = this.desiredVersionsHistory.map(v => v.time).sort()
                        if (history.length) {
                          this.timeSelect = history[history.length-1]
                          this.rerender()
                        }
                      }}
                    >
                      History
                    </Button>
                }/>
              <RefreshControl className={this.classes.control}
                              refresh={() => this.refresh()}
              />
            </FormGroup>
          }
          title={this.title}
        />
        <CardContent className={this.classes.content}>
          <div className={this.classes.inner}>
            <GridTable
              className={this.classes.versionsTable}
              columns={this.columns}
              rows={this.rows}
            />
            {this.error && <Alert className={this.classes.alert} severity="error">{this.error}</Alert>}
            {this.timeSelect ?
              <Box className={this.classes.controls}>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => { this.timeSelect = undefined; this.rerender() }}
                >
                  Cancel
                </Button>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => { this.timeSelect = undefined; this.rerender() }}
                >
                  Save
                </Button>
              </Box> : null}
          </div>
        </CardContent>
      </Card>
    )
  }
}

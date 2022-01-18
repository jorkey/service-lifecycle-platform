import {Box, Button, Card, CardContent, CardHeader, FormControlLabel, IconButton, Typography} from "@material-ui/core";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../common/components/refreshControl/RefreshControl";
import GridTable from "../../../common/components/gridTable/GridTable";
import React from "react";
import {GridTableColumnParams} from "../../../common/components/gridTable/GridTableColumn";
import UpIcon from "@material-ui/icons/ArrowUpward";
import DownIcon from '@material-ui/icons/ArrowDownward';
import {makeStyles} from "@material-ui/core/styles";
import {GridTableCellParams} from "../../../common/components/gridTable/GridTableCell";

export const useBaseStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  versionsTable: {
  },
  serviceColumn: {
    width: '150px',
    paddingLeft: '20px'
  },
  versionColumn: {
    width: '150px',
  },
  authorColumn: {
    width: '150px',
  },
  commentColumn: {
  },
  timeColumn: {
    width: '200px',
  },
  appearedAttribute: {
    color: 'green'
  },
  disappearedAttribute: {
    color: 'red'
  },
  modifiedAttribute: {
    fontWeight: 600
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
    marginRight: '10px',
    textTransform: 'none'
  },
  authorText: {
    textAlign: 'left',
    paddingLeft: 25,
    paddingTop: 2
  },
  timeText: {
    textAlign: 'left',
    paddingTop: 2
  },
  timeChangeButton: {
    width: '25px',
    textTransform: 'none',
  },
}));

type Classes = Record<"root" | "content" | "inner" | "versionsTable" |
                      "serviceColumn" | "versionColumn" | "authorColumn" | "timeColumn" | "commentColumn" |
                      "appearedAttribute" | "disappearedAttribute" | "modifiedAttribute" |
                      "controls" | "control" | "authorText" | "timeText" |
                      "timeChangeButton", string>

export interface ServiceVersion<Version> {
  service: string
  version: Version
}

export interface OptionServiceVersion<Version> {
  service: string
  version?: Version
}

export interface VersionInfo {
  author: string
  buildTime: string
  comment: string
}

export class DesiredVersionsView<Version> {
  private title: string
  private desiredVersionsHistory: {time:Date, author: string, versions:ServiceVersion<Version>[]}[]
  private versionsInfo: {version:ServiceVersion<Version>, info: VersionInfo}[]
  private compare: (v1: Version | undefined, v2: Version | undefined) => number
  private serialize: (version: Version) => string
  private parse: (version: string) => Version
  private modify: (desiredVersions: OptionServiceVersion<Version>[]) => Promise<any>
  private rerender: () => void
  private refresh: () => void
  private classes: Classes

  private desiredVersions: ServiceVersion<Version>[] = []
  private historyIndex: number = 0
  private time: Date | undefined
  private author: string | undefined

  constructor(title: string,
              desiredVersionsHistory: {time:Date, author: string, versions:ServiceVersion<Version>[]}[],
              versionsInfo: {version:ServiceVersion<Version>, info: VersionInfo}[],
              compare: (v1: Version | undefined, v2: Version | undefined) => number,
              serialize: (version: Version) => string,
              parse: (version: string) => Version,
              modify: (desiredVersions: OptionServiceVersion<Version>[]) => Promise<any>,
              rerender: () => void, refresh: () => void, classes: Classes) {
    this.title = title
    this.desiredVersionsHistory = desiredVersionsHistory
    this.versionsInfo = versionsInfo
    this.compare = compare
    this.serialize = serialize
    this.parse = parse
    this.modify = modify
    this.rerender = rerender
    this.refresh = refresh
    this.classes = classes
    this.setHistoryIndex(this.desiredVersionsHistory.length - 1)
  }

  getBaseColumns() {
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
        editable: true
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

  makeBaseRows() {
    return this.makeServicesList().map(service => {
      const modifiedVersion = this.desiredVersions.find(v => v.service == service)
      const currentVersion = this.getCurrentDesiredVersions().find(v => v.service == service)
      const version = modifiedVersion ? modifiedVersion : currentVersion!
      const appeared = !currentVersion && !!modifiedVersion
      const disappeared = !!currentVersion && !modifiedVersion
      const modified = !appeared && !disappeared && this.compare(modifiedVersion?.version, currentVersion?.version)
      const className = appeared?this.classes.appearedAttribute:
                        disappeared?this.classes.disappearedAttribute:
                        modified?this.classes.modifiedAttribute:
                        undefined
      const info = this.versionsInfo.find(info => info.version.service == service &&
        this.compare(info.version.version, version?.version) == 0)?.info
      return new Map<string, GridTableCellParams>([
        ['service', {
          value: service,
          className: className
        }],
        ['version', {
          value: this.serialize(version.version),
          className: className,
          editable: !disappeared,
          select: !disappeared?this.versionsInfo.filter(v => v.version.service == service)
            ?.map(v => ({value: this.serialize(v.version.version),
              description: this.serialize(v.version.version) + ' - ' + v.info.comment } as {value:string, description:string})):undefined
        }],
        ['author', {
          value: info?.author,
          className: className
        }],
        ['buildTime', {
          value: info?.buildTime,
          className: className
        }],
        ['comment', {
          value: info?.comment,
          className: className
        }]
      ])})
  }

  render(columns: GridTableColumnParams[], rows: Map<string, GridTableCellParams>[]) {
    return (
      <Card
        className={this.classes.root}
      >
        <CardHeader
          action={
            <FormGroup row>
              <FormControlLabel
                label={null}
                control={<>
                  <IconButton
                    className={this.classes.timeChangeButton}
                    disabled={this.historyIndex == 0}
                    onClick={() => {
                      this.setHistoryIndex(this.historyIndex -= 1)
                      this.rerender()
                    }}>
                    <DownIcon/>
                  </IconButton>
                </>
                }
              />
              <FormControlLabel
                label={null}
                control={<>
                  <IconButton
                    className={this.classes.timeChangeButton}
                    disabled={this.historyIndex == this.desiredVersionsHistory.length-1}
                    onClick={() => {
                      this.setHistoryIndex(this.historyIndex += 1)
                      this.rerender()
                    }}>
                    <UpIcon/>
                  </IconButton>
                </>
                }
              />
              <FormControlLabel
                label={null}
                control={
                  <Typography className={this.classes.timeText}>Time: {this.time?.toLocaleString()}</Typography>
                }
              />
              <FormControlLabel
                label={null}
                control={
                  <Typography className={this.classes.authorText}>Author: {this.author}</Typography>
                }
              />
              <RefreshControl className={this.classes.control}
                              refresh={() => { this.refresh() }}
              />
            </FormGroup>
          }
          title={this.title}
        />
        <CardContent className={this.classes.content}>
          <div className={this.classes.inner}>
            <GridTable
              className={this.classes.versionsTable}
              columns={columns}
              rows={rows}
              onRowChanged={ (row, values, oldValues) =>
                new Promise(resolve => {
                  const service = values.get('service')
                  const version = this.parse(values.get('version') as string)
                  this.desiredVersions = this.desiredVersions?.map(v => {
                    if (v.service == service) {
                      return {service: v.service, version: version}
                    } else {
                      return v
                    }
                  })
                  this.rerender()
                  resolve(true)
                })
              }
            />
            {this.historyIndex != this.desiredVersionsHistory.length - 1 || this.isModified() ?
              <Box className={this.classes.controls}>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => {
                          this.setHistoryIndex(this.desiredVersionsHistory.length - 1)
                          this.rerender()
                        }}
                >
                  Cancel
                </Button>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        disabled={!this.isModified()}
                        onClick={() => {
                          this.modify(this.getDeltas()).then(() => this.refresh())
                        }}
                >
                  Save
                </Button>
              </Box> : null}
          </div>
        </CardContent>
      </Card>
    )
  }

  private makeServicesList() {
    const services = new Set<string>()
    this.getCurrentDesiredVersions().map(v => v.service).forEach(s => services.add(s))
    this.desiredVersions!.map(v => v.service).forEach(s => services.add(s))
    return Array.from(services).sort()
  }

  private getCurrentDesiredVersions() {
    return this.desiredVersionsHistory![this.desiredVersionsHistory!.length - 1].versions
  }

  private setHistoryIndex(index: number) {
    this.historyIndex = index
    const current = this.desiredVersionsHistory![index]
    this.time = current.time
    this.author = current.author
    this.desiredVersions = [...current.versions]
  }

  private isModified() {
    return this.desiredVersions?.length != this.getCurrentDesiredVersions().length ||
           this.desiredVersions?.find(v1 => {
             const v2 = this.getCurrentDesiredVersions().find(v2 => v2.service == v1.service)
             return this.compare(v1.version, v2?.version) != 0
           })
  }

  private getDeltas() {
    const current = this.getCurrentDesiredVersions()
    const modified = this.desiredVersions.filter(v => !current.find(v1 => v.service == v1.service &&
      this.compare(v.version, v1.version) == 0)) as OptionServiceVersion<Version>[]
    const removed = current.filter(v => !this.desiredVersions.find(v1 => v.service == v1.service))
      .map(v => { return {service: v.service, version: undefined} as OptionServiceVersion<Version>})
    const deltas = [...modified, ...removed]
    return deltas
  }
}

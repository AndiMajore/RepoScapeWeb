<template>
  <div>
    <v-container>
      <v-card style="margin:5px">
        <v-card-title>History<span
          style="color:gray; padding-left: 7px"> ({{
            options.favos ? "Favourites" : options.chronological ? "List" : "Tree"
          }})</span></v-card-title>
        <v-container ref="history">
          <v-row>
            <v-col>
              <v-treeview
                :items="getHistoryList()"
                hoverable
                activatable
                dense
                shaped
                :active="selection"
                expand-icon="fas fa-angle-down"
                :transition="true"
                v-on:update:active="handleSelection"
              >
                <template v-slot:prepend="{item}">
                  <v-list-item>
                    <v-tooltip top>
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn
                          icon
                          :disabled="item.id === current || (item.state !== undefined &&item.state !== 'DONE')"
                          @click="loadGraph(item.id)"
                          v-bind="attrs"
                          v-on="on"
                        >
                          <v-icon
                            :color="item.id === current ? 'gray': item.state===undefined ?'primary':'green'"
                          >
                            far fa-play-circle
                          </v-icon>
                        </v-btn>
                      </template>
                      <span>{{ item.state !== undefined ? item.method : "MANUAL" }}</span>
                    </v-tooltip>

                    <v-divider vertical style="margin: 10px"></v-divider>
                    <span style="color: darkgray; font-size: 10pt">
                    <v-tooltip top>
                      <template v-slot:activator="{ on, attrs }">
                        <v-icon
                          size="17px"
                          color="primary"
                          dark
                          v-bind="attrs"
                          v-on="on"
                        >
                          far fa-clock
                        </v-icon>
                      </template>
                    <span class="noselect">{{ formatTime(item.created)[0] }}</span>
                  </v-tooltip>

                    ({{ formatTime(item.created)[1] }} ago)


                  </span>
                  </v-list-item>
                </template>
                <template v-slot:label="{item}"
                >
                  <span class="noselect; cut-text; text-button">{{ getName(item) }}</span>
                </template>

              </v-treeview>
            </v-col>
            <v-divider vertical></v-divider>
            <v-col v-if="selectedId!==undefined">
              <v-card v-if="selected===undefined" style="padding-bottom: 15px">
                <v-card-title>{{ selectedId }}</v-card-title>
                <v-progress-circular
                  color="primary"
                  size="50"
                  width="5"
                  indeterminate
                ></v-progress-circular>
              </v-card>
              <v-card v-else style="padding-bottom: 15px">
                <v-container>
                  <v-row v-if="selected.parentId !=null">
                    <v-timeline align-top dense
                                style="margin-top: 10px; padding-top: 7px; margin-bottom: -29px; margin-left: -25px">
                      <v-timeline-item
                        right
                        :small="!hoveringTimeline('parent')"
                        :color="selected.parentMethod !=null? 'green':'primary'"
                      >
                        <v-btn plain style="color: gray; margin-left: -15px; margin-bottom:-25px; margin-top:-25px"
                               @click="handleSelection([selected.parentId])">{{ selected.parentName }}
                        </v-btn>
                        <br><br>
                        <template v-slot:icon>
                          <v-btn icon @click="loadGraph(selected.parentId)">
                            <v-icon x-small color="white">
                              fas fa-play
                            </v-icon>
                          </v-btn>
                        </template>
                      </v-timeline-item>
                    </v-timeline>
                  </v-row>
                  <v-row dense>
                    <v-col cols="1">
                      <v-btn icon :color="this.selected.method!=null ? 'green':'primary'" x-large
                             style="background-color: white; margin-top: 5px;" @click="loadGraph(selectedId)">
                        <v-icon>far fa-play-circle</v-icon>
                      </v-btn>
                    </v-col>
                    <v-col cols="9">
                      <v-card-title v-if="!edit">{{ selected.name }}</v-card-title>
                      <v-text-field v-else :placeholder="selected.name" :value="selected.name" label="Name"
                                    @change="setName" style="margin-left: 20px"></v-text-field>
                    </v-col>
                    <v-col cols="2" style="padding-top: 15px">
                      <v-btn v-show="selected.owner = $cookies.get('uid')" icon style="margin-top:10px" x-small
                             @click="toggleEdit()">
                        <v-icon>{{ edit ? "fas fa-check" : "fas fa-edit" }}</v-icon>
                      </v-btn>
                      <v-btn icon style="margin-top:10px" @mouseover="hover.star=true"
                             @mouseleave="hover.star=false"
                             x-small
                             @click="toggleStar">
                        <v-icon v-if="showStar(false)">far fa-star</v-icon>
                        <v-icon v-if="showStar(true)">fas fa-star</v-icon>
                      </v-btn>
                      <v-btn v-show="selected.owner = $cookies.get('uid')" icon style="margin-top:10px" x-small
                             @click="deletePopup=true">
                        <v-icon>
                          fas fa-trash
                        </v-icon>
                      </v-btn>
                    </v-col>
                  </v-row>

                  <v-divider
                    style="margin: -10px 15px -10px 10px;"></v-divider>
                  <v-row dense v-if="Object.keys(selected.children).length>0">
                    <v-timeline align-top dense
                                style="margin-top: 10px; padding-top: 7px; padding-bottom: 0px; margin-left: 5px">
                      <v-timeline-item small right v-for="child in Object.keys(selected.children)" :key="child"
                                       :color="selected.children[child].method !==undefined ? 'green':'primary'"
                      >
                        <v-btn plain style="color: gray; margin-left: -15px; margin-bottom:-25px; margin-top:-25px"
                               @click="handleSelection([child])">{{ selected.children[child].name }}
                        </v-btn>
                        <template v-slot:icon>
                          <v-btn icon @click="loadGraph(child)" style="margin-left:2px">
                            <v-icon x-small color="white">
                              fas fa-play
                            </v-icon>
                          </v-btn>
                        </template>
                      </v-timeline-item>
                    </v-timeline>
                  </v-row>
                  <v-row style="margin: 25px"></v-row>
                  <v-row style="margin: 25px" v-if="selected.jobid!=null">
                    <v-chip outlined @click="downloadJob(selected.jobid)">
                      {{ selected.method }} Results
                      <v-icon right>fas fa-download</v-icon>
                    </v-chip>

                  </v-row>
                  <v-row v-if="$global.metagraph!=null &&selected!==undefined">
                    <v-container>
                      <!--                      <v-card-title>General Information</v-card-title>-->
                      <v-row>
                        <v-col>
                          <v-list>
                            <v-list-item>
                              <b>Nodes ({{ getCounts('nodes') }})</b>
                            </v-list-item>
                            <v-list-item v-for="node in Object.keys(selected.counts.nodes)"
                                         :key="node">
                              <v-chip outlined>
                                <v-icon left :color="getExtendedColoring('nodes',node)">fas fa-genderless</v-icon>
                                {{ node }} ({{ selected.counts.nodes[node] }})
                              </v-chip>
                            </v-list-item>

                          </v-list>
                        </v-col>
                        <v-col>
                          <v-list>
                            <v-list-item>
                              <b>Edges ({{ getCounts('edges') }})</b>
                            </v-list-item>
                            <v-list-item v-for="edge in Object.keys(selected.counts.edges)" :key="edge">
                              <v-chip outlined>
                                <v-icon left :color="getExtendedColoring('edges',edge)[0]">fas fa-genderless</v-icon>
                                <template v-if="directionExtended(edge)===0">
                                  <v-icon left>fas fa-undo-alt</v-icon>
                                </template>
                                <template v-else>
                                  <v-icon v-if="directionExtended(edge)===1" left>fas fa-long-arrow-alt-right</v-icon>
                                  <v-icon v-else left>fas fa-arrows-alt-h</v-icon>
                                  <v-icon left :color="getExtendedColoring('edges',edge)[1]">fas fa-genderless</v-icon>
                                </template>
                                {{ edge }} ({{ selected.counts.edges[edge] }})

                              </v-chip>

                            </v-list-item>
                          </v-list>
                        </v-col>
                      </v-row>


                    </v-container>
                  </v-row>
                  <v-divider></v-divider>
                  <v-row v-if="selected.thumbnailReady" style="padding:15px;display: flex;justify-content: center">
                    <v-img max-height="600" max-width="600" :src="getThumbnail(selectedId)">
                    </v-img>
                  </v-row>
                  <v-row v-else style="padding:15px;display: flex;justify-content: center" >
                    <i style="color:gray">creating preview...</i>
                    <v-progress-linear
                      indeterminate
                      color="primary"
                    ></v-progress-linear>
                  </v-row>
                  <v-row style="margin-top:10px">
                      <v-textarea outlined label="Description" @change="updateDesc" :value="description" rows="5"
                                  no-resize style="margin: 15px">
                        <template v-slot:append>
                          <v-container style="margin-right: -5px">
                            <v-row>
                              <v-btn icon @click="saveDescription(true)" :disabled="!showSaveDescription()">
                                <v-icon color="green">fas fa-check</v-icon>
                              </v-btn>
                            </v-row>
                            <v-row>
                              <v-btn icon @click="saveDescription(false)" :disabled="!showSaveDescription()">
                                <v-icon color="red">far fa-times-circle</v-icon>
                              </v-btn>
                            </v-row>
                          </v-container>
                        </template>
                      </v-textarea>
                  </v-row>
                </v-container>
              </v-card>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-container>
    <v-dialog
      v-model="deletePopup"
      persistent
      max-width="500"
    >
      <v-card>
        <v-card-title>Confirm Deletion</v-card-title>
        <v-card-text>Do you really want to delete the selected Network?
        </v-card-text>
        <v-divider></v-divider>

        <v-card-actions>
          <v-btn
            color="green darken-1"
            text
            @click="closeDeletePop(false)"
          >
            Dismiss
          </v-btn>
          <v-btn
            color="red darken-1"
            text
            @click="closeDeletePop(true)"
          >
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>

    </v-dialog>
  </div>
</template>

<script>
import * as CONFIG from "../../Config"

export default {
  name: "History.vue",
  props: {
    options: Object,
  },
  data() {
    return {
      history: [],
      current: undefined,
      sort: false,
      showOtherUsers: false,
      user: undefined,
      list: [],
      reverseSorting: false,
      selection: [],
      description: "",
      selectedId: undefined,
      selected: undefined,
      hover: {star: false, timeline: {parent: false, children: {}}},
      edit: false,
      deletePopup: false,
    }
  },

  created() {
    this.$socket.$on("thumbnailReady", this.thumbnailReady)
    this.init()
  },

  methods: {
    init: function () {
      if (this.user === undefined)
        this.user = this.$cookies.get("uid")
      this.loadHistory()
    },
    reload: function () {
      this.loadHistory()
    },

    loadHistory: function () {
      this.current = this.$route.params["gid"];
      if (this.user !== undefined)
        this.$http.get("/getUser?user=" + this.$cookies.get("uid")).then(response => {
          if (response.data !== undefined)
            return response.data;
        }).then(data => {
          this.history = data.history.sort((a, b) => {
            return b.created - a.created
          });
          this.list = data.chronology;
        }).then(() => {
          this.handleSelection([this.current])
        }).catch(err => console.log(err))
    },
    loadGraph: function (graphid) {
      this.$emit("graphLoadEvent", {post: {id: graphid}})
    },
    hoveringTimeline: function (label) {
      return this.hover[label]
    },
    getName: function (item) {
      if (item.name === item.id) {
        let edges = 0;
        Object.values(item.edges).forEach(i => edges += i)
        let nodes = 0;
        Object.values(item.nodes).forEach(i => nodes += i)
        return "Edges: " + edges + "[" + Object.keys(item.edges).length + "]; Nodes: " + nodes + "[" + Object.keys(item.nodes).length + "]"
      }
      return item.name
    },
    directionExtended: function (edge) {
      let e = Object.values(this.selected.entityGraph.edges).filter(e => e.name === edge)[0];
      if (e.node1 === e.node2)
        return 0
      return e.directed ? 1 : 2
    },

    thumbnailReady: function (response) {
      let params = JSON.parse(response)
      if (params.gid === this.selectedId) {
        this.selected.thumbnailReady = true
      }
      this.$socket.unsubscribeThumbnail(params.gid)
    },
    getExtendedColoring: function (entity, name) {
      return this.$utils.getColoringExtended(this.$global.metagraph, this.selected.entityGraph, entity, name)
    },
    handleSelection: function (selected) {
      if (selected[0] === undefined || this.selectedId === selected[0])
        return
      this.selection = selected
      this.selected = undefined;
      this.selectedId = selected[0]
      this.$http.get("getGraphHistory?gid=" + this.selectedId + "&uid=" + this.$cookies.get("uid")).then(response => {
        if (response.data !== undefined)
          return response.data
      }).then(data => {
        this.selected = data
        this.description = data.comment
      }).then(() => {
        if (!this.selected.thumbnailReady) {
          this.$socket.subscribeThumbnail(this.selectedId, "thumbnailReady")
        }
      }).catch(console.log)
    },
    closeDeletePop: function (apply) {
      this.deletePopup = false
      if (!apply)
        return
      this.$http.get("removeGraph?gid=" + this.selectedId).then(() => {
        if (this.current === this.selectedId)
          this.$router.push("/" + this.current + "/history")
        else
          this.reload()
      })
    },

    updateDesc: function (event) {
      this.description = event
    },
    showSaveDescription: function () {
      return this.description !== this.selected.comment
    },
    saveDescription: function (apply) {
      if (apply) {
        this.$http.post("saveGraphDescription", {"gid": this.selectedId, "desc": this.description}).then(() => {
          this.selected.comment = this.description
        })
      } else
        this.description = this.selected.comment
    },
    getCounts: function (entity) {
      let objects = Object.values(this.selected.counts[entity]);
      return objects === undefined || objects.length === 0 ? 0 : objects.reduce((i, j) => i + j)
    },

    formatTime: function (timestamp) {
      timestamp *= 1000
      let d = new Date();
      let date = new Date(timestamp);
      let diff = ((d.getTime() - (d.getTimezoneOffset() * 60 * 1000)) - timestamp) / 1000;
      let diff_str = "~";
      if (diff < 60)
        diff_str = "<1m";
      else if (diff < 60 * 60)
        diff_str += Math.round(diff / (60)) + "m";
      else if (diff < 60 * 60 * 24)
        diff_str += Math.round(diff / (60 * 60)) + "h";
      else if (diff < 60 * 60 * 24 * 365)
        diff_str += Math.round(diff / (60 * 60 * 24)) + "d";
      else
        diff_str += Math.round(diff / (60 * 60 * 24 * 356)) + "a";
      return [date.toISOString().slice(0, -8), diff_str]
    },

    showStar: function (starred) {
      if (starred) {
        return (this.hover.star && !this.selected.starred) || (this.selected.starred && !this.hover.star)
      }
      return (this.hover.star && this.selected.starred) || (!this.selected.starred && !this.hover.star)
    },

    toggleStar: function () {
      this.selected.starred = !this.selected.starred;
      this.$http.get("toggleStarred?uid=" + this.$cookies.get("uid") + "&gid=" + this.selectedId).catch(console.log)
    },

    saveName: function () {
      this.getGraphsWithId(this.selectedId, this.history)[0].name = this.selected.name
      this.getGraphsWithId(this.selectedId, this.list)[0].name = this.selected.name
      this.$http.post("setGraphName", {gid: this.selectedId, name: this.selected.name}).catch(console.log)
    },

    getGraphsWithId: function (id, list) {
      let out = []
      list.forEach(h => {
        if (h.id === id)
          out.push(h)
        if (h.children !== undefined && h.children.length > 0) {
          let rest = this.getGraphsWithId(id, h.children)
          rest.forEach(r => out.push(r))
        }
      })
      return out;
    },

    getHistoryList: function () {
      let out = this.history;
      if (this.options.chronological) {
        out = this.getChronologicalList()
      }
      if (this.options.favos) {
        out = this.getChronologicalList().filter(l=>l.starred)
      }
      return out

    },

    getChronologicalList: function () {
      if (!this.otherUsers)
        return this.list.filter(l => l.user === this.user)
      else
        return this.list
    },


    getThumbnail: function (graph_id) {
      return CONFIG.HOST_URL+CONFIG.CONTEXT_PATH+"/api/getThumbnailPath?gid=" + graph_id
    },

    // thumbnailExists: function (graph_id) {
    //   let http = new XMLHttpRequest()
    //   http.open("HEAD", this.getThumbnail(graph_id), false)
    //   let status = 2
    //   return http.onloadend = function () {
    //     if (http.status === 404) {
    //       status = 2
    //       return false
    //     } else
    //       return true
    //   }
    // },
    downloadJob: function (jobid) {
      window.open(CONFIG.HOST_URL+CONFIG.CONTEXT_PATH+'/api/downloadJobResult?jid=' + jobid)
    },

    reverseList: function () {
      this.list = this.list.reverse()
    },

    setName: function (newName) {
      this.selected.name = newName
    },

    toggleEdit: function () {
      if (this.edit)
        this.saveName()
      this.edit = !this.edit;
    }


  },


}
</script>

<style lang="sass">

.cut-text
  text-overflow: ellipsis
  overflow: hidden
  width: 160px
  height: 1.2em
  white-space: nowrap

.noselect
  -webkit-touch-callout: none
    -webkit-user-select: none
      -khtml-user-select: none
        -moz-user-select: none
          -ms-user-select: none
            user-select: none


</style>

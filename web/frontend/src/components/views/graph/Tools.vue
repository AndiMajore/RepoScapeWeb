<template>
  <v-card ref="tools" elevation="3" v-if="entityGraph!==undefined">
    <v-container v-show="show">
      <v-list ref="list">
        <v-list-item>
          TODO
        </v-list-item>
      </v-list>
    </v-container>
  </v-card>
</template>

<script>


export default {
  props: {
    countMap: undefined,
    entityGraph: undefined,
    options: Object,
  },
  name: "Tools",

  data() {
    return {
      show: true,
      tabModel: 0,
      nodeMap: {}
    }
  },
  created() {
    if (this.options.toggled === undefined) {
      this.options.toggled = {}
    }
  },
  methods: {


  }
}
</script>

<style scoped>

</style>

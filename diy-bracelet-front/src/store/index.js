import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || ''
  },
  mutations: {
    setToken (state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    setUsername (state, username) {
      state.username = username
      localStorage.setItem('username', username)
    },
    clearUser (state) {
      state.token = ''
      state.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
    }
  },
  actions: {
    login ({ commit }, { token, username }) {
      commit('setToken', token)
      commit('setUsername', username)
    },
    logout ({ commit }) {
      commit('clearUser')
    }
  }
})

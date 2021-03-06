/**
 * Created by Frezc on 2016/6/8.
 */
import { AsyncStorage } from 'react-native'
import AppWidgets from '../libs/AppWidgets'

export function saveSchedule() {
  return (dispatch, getState) => {
    const sp = getState().view.schedulePage
    sp.ready = true
    return AsyncStorage.setItem('schedule', JSON.stringify(sp))
      .then(err => {
        AppWidgets.update()
        console.log('err ' + err)
        return err
      })
  }
}

export function saveTodos() {
  return (dispatch, getState) => {
    const todos = getState().todos
    return AsyncStorage.setItem('todos', JSON.stringify(todos))
      .then(err => {
        AppWidgets.update()
        console.log('err ' + err)
        return err
      })
  }
}

export function saveScheduleAndTodos() {
  return (dispatch, getState) => {
    const sp = getState().view.schedulePage
    sp.ready = true
    const todos = getState().todos
    return Promise.all([
      AsyncStorage.setItem('schedule', JSON.stringify(sp)),
      AsyncStorage.setItem('todos', JSON.stringify(todos))
    ]).then(err => {
      AppWidgets.update()
      console.log('err ' + err)
      return err
    })
  }

}

export function saveHistory() {
  return (dispatch, getState) => {
    const hp = getState().view.historyPage
    hp.ready = true
    return AsyncStorage.setItem('history', JSON.stringify(hp))
      .then(err => {
        console.log('err ' + err)
        return err
      })
  }
}

export function clearData() {
  return dispatch => {
    return AsyncStorage.clear()
      .then(err => {
        AppWidgets.update()
        return err
      })
  }
}

export function saveAuth() {
  return (dispatch, getState) => {
    const auth = getState().auth
    return AsyncStorage.setItem('auth', JSON.stringify(auth))
  }
}

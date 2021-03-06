import { ToastAndroid } from 'react-native';
import { APPIDENTITY } from '../constants'

export function generateRandomStringArray(length, str = 'random') {
  let array = new Array(length)
  for(let i = 0; i < length; i++) {
    array[i] = str
  }
  
  return array
}

/**
 * fetch with auto retry
 * @param url request url
 * @param params fetch params with { retry(int, default 3), deltaTime(int, default 1000(ms)) }
 */
export function fetchR(url, params) {
  params = Object.assign({
    retry: 3,
    deltaTime: 1000
  }, params);

  return new Promise((resolve, reject) => {
    const wrappedFetch = (retry) => {
      // ToastAndroid.show('fetch ' + retry + url, ToastAndroid.SHORT)
      fetch(url, params)
        .then(response => {
          resolve(response);
        })
        .catch(error => {
          // ToastAndroid.show(retry + ' ' + error.message, ToastAndroid.SHORT)

          if (retry > 1) {
            setTimeout(() => {
              wrappedFetch(retry - 1);
            }, params.deltaTime);
          } else {
            reject(error);
          }
        })
    };
    wrappedFetch(params.retry);
  });
}

/**
 * auto construct query string with param app='todolite_android'
 * @param url
 * @param query query object
 * @param params
 */
export function easyFetch(url, query = {}, params) {
  const qs = constructQuery(Object.assign({
    app: APPIDENTITY
  }, query))
  return fetchR(`${url}?${qs}`, params)
}

/**
 * resolve laravel validation error object
 * {
 *   [param name]: ["error messages", ..]
 * }
 * @param errMsg array ["error messages"]
 */
export function resolveValidationError(errMsg) {
  let messages = []
  for (const msgs of Object.values(errMsg)) {
    for (const msg of msgs) {
      messages.push(msg)
    }
  }
  return messages
}

/**
 * handle error response
 * @param response
 */
export function resolveErrorResponse(response) {
  switch (response.status) {
    case 430:
      ToastAndroid.show('Wrong code. Please retry.', ToastAndroid.LONG)
      break
    case 431:
      ToastAndroid.show('Code is expired. Please re-send email.', ToastAndroid.LONG)
      break
    case 400:
      response.json().then(json => {
        ToastAndroid.show(resolveValidationError(json.error).join(' '), ToastAndroid.LONG)
      })
      break
    case 404:
    case 403:
    case 401:
      response.json().then(json => {
        ToastAndroid.show(json.error, ToastAndroid.LONG)
      })
      break
    default:
      response.text().then(text => {
        ToastAndroid.show(text, ToastAndroid.LONG)
      })
  }
  return response.status
}

/**
 * construct query string by params obj
 * @param params
 */
export function constructQuery(params = {}) {
  const query = []
  for (const key of Object.keys(params)) {
    query.push(key + '=' + params[key])
  }
  return query.join('&')
}

/**
 * construct form data body
 * @param params
 * @returns {FormData}
 */
export function constructBody(params = {}) {
  const formData = new FormData()
  for (const key of Object.keys(params)) {
    formData.append(key, params[key])
  }
  return formData
}

const SECOND_TIME = 1000
const MINUTE_TIME = 60 * 1000
const HOUR_TIME = 60 * MINUTE_TIME
const DAY_TIME = 24 * HOUR_TIME
const MONTH_TIME = 30 * DAY_TIME

/**
 * format date to string
 * @param date
 */
export function formatDate(date) {
  const now = Date.now()
  const target = date.getTime()
  const offset = Math.abs(target - now)
  const chrono = now > target ? 'ago' : 'after'
  if (offset < MINUTE_TIME) {
    return `${Math.floor(offset / SECOND_TIME)} sec ${chrono}`
  } else if (offset < HOUR_TIME) {
    return `${Math.floor(offset / MINUTE_TIME)} min ${chrono}`
  } else if (offset < DAY_TIME) {
    return `${Math.floor(offset / HOUR_TIME)} h ${chrono}`
  } else if (offset < MONTH_TIME) {
    return `${Math.floor(offset / DAY_TIME)} day ${chrono}`
  }


  return formatDateNormal(date)
}

/**
 * format date with yyyy-MM-dd hh:mm a
 * @param date
 * @returns {string}
 */
export function formatDateNormal(date) {
  const year = date.getFullYear()
  const month = `0${date.getMonth() + 1}`.slice(-2)
  const day = `0${date.getDate()}`.slice(-2)
  const hour = date.getHours() > 12 ? date.getHours() - 12 : date.getHours()
  const minute = `0${date.getMinutes()}`.slice(-2)
  const ampm = date.getHours() > 12 ? 'pm' : 'am'

  return `${year}-${month}-${day} ${hour}:${minute} ${ampm}`
}

/**
 * This function will return a new array after insert.
 * @param originArr origin array
 * @param insertArr the array to insert
 * @param offset the inserting position of orgin array
 */
export function arrayInsert(originArr, insertArr, offset) {
  return originArr.slice(0, offset).concat(insertArr).concat(originArr.slice(offset))
}

/**
 * return a new array that removed first element param (if exist).
 * or return origin array. (if not exist)
 * @param arr origin array won't be modified
 * @param element
 */
export function arrayRemove(arr, element) {
  const index = arr.indexOf(element)
  if (index === -1) {
    return arr
  } else {
    return arr.slice(0, index).concat(arr.slice(index + 1))
  }
}

import axios from 'axios'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    Accept: 'application/json',
  },
})

let accessTokenProvider = () => null

export function setAccessTokenProvider(provider) {
  if (typeof provider !== 'function') {
    throw new TypeError(
      'Access token provider должен быть функцией'
    )
  }

  accessTokenProvider = provider
}

http.interceptors.request.use(
  (config) => {
    const token = accessTokenProvider?.()

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error),
)

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)

export default http

import axios from "axios"

export default class HSAdmin {
    constructor(options = {}) {
        this._ax = axios.create({
            baseURL: options.baseURL || "http://localhost:45678/api", // TODO: change to production URL
        });
    }

    get token() { return this._token; }

    async get(endpoint, config = {}) {
        return this._ax.get(endpoint, config);
    }

    async post(endpoint, data) {
        return this._ax.post(endpoint, data);
    }

    async authenticate(user = "admin", pass = "admin") {
        const res = await this.post("authenticate", { username: user, password: pass});
        if (!res.data.id_token) {
            throw new Error("authentication response is missing id_token value");
        }
        this._token = res.data.id_token;
        this._ax.defaults.headers.common["Authorization"] = "Bearer " + this._token;
    }
}

import Vue from 'vue'
import Router from 'vue-router'
import Home from './views/Home.vue'
import HSAdmin from "./hsadmin.js"

Vue.use(Router);

const hsa = new HSAdmin();
const routeProps = (route) => {
    return Object.assign({}, route.params,{
        hsadmin: hsa
    });
};

export default new Router({
    mode: 'history',
    base: process.env.BASE_URL,
    routes: [
        {
            path: '/',
            name: 'home',
            component: Home
        },
        {
            path: "/login",
            name: "login",
            component: () => import(/* webpackChunkName: "login" */ "./views/Login.vue"),
            props: routeProps,
        },
        {
            path: "/customers",
            name: "customers",
            component: () => import(/* webpackChunkName: "customers" */ "./views/Customers.vue"),
            props: routeProps,
        },
        {
            path: "/customers/:id",
            name: "customer",
            component: () => import(/* webpackChunkName: "customers" */ "./views/Customer.vue"),
            props: routeProps,
        },
    ]
})

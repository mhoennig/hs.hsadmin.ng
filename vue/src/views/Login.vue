<template>
    <div class="hsa-login">
        <!-- TODO: the <input>s need unique IDs in order to create labels, see e.g. https://stackoverflow.com/a/50121385/417040 -->
        <input type="text" class="user" v-model="user" :disabled="working" />:<input type="password" class="pass" v-model="pass" :disabled="working" />
        <button v-on:click="clickHandler" :disabled="working">Log In</button>
        <div v-if="error">{{ error }}</div>
    </div>
</template>

<script>
    import HSAdmin from "../hsadmin";
    export default {
        name: "Login",
        props: { hsadmin: HSAdmin },
        data () { return {
            user: "admin",
            pass: "admin",
            error: false,
            working: false,
        }},
        methods: {
            clickHandler: async function () {
                this.error = false;
                this.working = true;
                try {
                    await this.hsadmin.authenticate(this.user, this.pass);
                    this.working = false;
                } catch (error) {
                    this.working = false;
                    this.error = error;
                    return;
                }
                this.$router.push({name: "home"});
            }
        },
    }
</script>

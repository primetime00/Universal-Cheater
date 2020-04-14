spa.$('main', {

    state: 0,
    system: '',
    game: {},
    intervalRunning: false,

    dataPreProcessAsync: function(){
        this.installStatusCheck();
        return axios.get('getCheatStatus')
            .then(function(resp) {
                return {status: resp.data};
            });
    },

    dataProcess: function(initial, dt) {
        if (dt.status.state == 2) {
            app.main.state = dt.status.state;
            app.main.system = dt.status.system;
            app.main.game = {system: dt.status.system, game: dt.status.game, cht: dt.status.cht };
            app.header.setTitle(dt.status.system + '/' + dt.status.game, true);
        }
        return {state: app.main.state};
    },


    setSystem: function(sys) {
        this.system = sys;
        app.header.setTitle(sys, true);
        app.header.setBackFunction();
        this.setState(1);
    },

    setGame: function(game) {
        this.game = game;
        app.header.setTitle(this.system + '/' + game.game, true);
        this.setState(2);
    },

    setState: function(st) {
        this.state = st;
        spa.$render('main');
    },

    exitCheat: function(newState) {
        console.log("exiting cheat");
        app.cheat.exitCheat()
            .then(function() {
                app.main.setState(1);
            })
    },

    backClick: function() {
        console.log('back was clicked');
        switch (this.state) {
            case 1:
                this.system = "";
                this.game = {};
                app.header.setTitle("", false);
                this.setState(0);
                break;
            case 2:
                this.game = {};
                app.header.setTitle(this.system, true);
                this.exitCheat(1)
                break;
            default:
                break;
        }
    },

    onRender: function () {
        if (this.state == 0) {
            spa.$render('systems', {
                target: '#stageContainer'
            });
        }
        else if (this.state == 1) {
            spa.$render('games', {
                target: '#stageContainer'
            });
        }
        else if (this.state == 2) {
            spa.$render('cheat', {
                target: '#stageContainer'
            });
        }

    },

    checkStatus: function() {
        return axios.get('getCheatStatus')
            .then(function(resp) {
                app.main.processStatus(resp.data);
                if (app.cheat != undefined) {
                    app.cheat.processStatus(resp.data);
                }
                setTimeout(app.main.checkStatus, 1000);
            });
    },

    installStatusCheck: function() {
        if (!app.main.intervalRunning) {
            app.main.intervalRunning = true;
            console.log("installing status checker");
            app.main.checkStatus();
        }
    },


    processStatus: function(status) {
        var previousState = app.main.state;
        var currentState = status.state;
        if (previousState < 2 && currentState < 2) return;
        if (previousState == 2 && currentState == 2) return;
        switch (currentState) {
            case 2:
                app.main.state = status.state;
                app.main.system = status.system;
                app.main.game = {system: status.system, game: status.game, cht: status.cht };
                app.header.setTitle(status.system + '/' + status.game, true);
                break;
            default:
                app.main.state = status.state;
                app.main.system = "";
                app.main.game = {};
                app.header.setTitle("", false);
                app.main.setState(0);
                break;
        }
    },
});


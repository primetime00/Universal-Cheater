spa.$('cheat', {
    dataPreProcessAsync: function(){
        document.addEventListener('prechange', function(event) {
          document.querySelector('ons-toolbar .center')
            .innerHTML = event.tabItem.getAttribute('label');
        });

        return axios.post('runGameCheat', {"system": app.main.game.system, "cht": app.main.game.cht })
            .then(this.getCheatStatus)
            .then(function(resp) {
                console.log('done', resp.data.cheatList)
                return {cheatList: resp.data.cheatList};
            })
    },

    hash: 0,

    processStatus: function(status) {
        var nHash = status.hash;
        if (nHash != app.cheat.hash) {
            app.cheat.hash = nHash;
            spa.$refresh('cheat', {
                onRefresh: function() {
                    return {cheatList: status.cheatList};
                }
            });
        }
        console.log("cheat", status, app.cheat.hash, nHash);
    },

    dataProcess: function(initial, cheatList) {
        return cheatList;
    },

    getCheatStatus: function() {
        return axios.get('getCheatStatus');
    },

    exitCheat: function () {
        return axios.post('exitCheat');
    },

    onChange: function(value, index) {
        console.log("change", value, index);
        axios.post('toggleGameCheat', index)
            .then(function(resp) {
                console.log('done', resp.data)
            });
    },

    onTrigger: function(value, index) {
        console.log("trigger", value, index);
        axios.post('triggerGameCheat', index)
            .then(function(resp) {
                console.log('done', resp.data)
            });
    },


    onReset: function(value, index) {
        console.log("reset", value, index);
        axios.post('resetGameCheat', index)
            .then(function(resp) {
                console.log('done', resp.data)
            });
    }

});


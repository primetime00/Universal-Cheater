spa.$('games', {

    dataPreProcessAsync: function(){
        return axios.get('getGameCheats/'+app.main.system)
            .then(function(resp) {
                return {data: resp.data};
            })
    },

    dataProcess: function(initial, resp) {
        if (resp.data.status != 'SUCCESS') {
            console.log("there are no games!", resp.data.gameList);
        }
        return {
            games: resp.data.gameList
        }
    },

    gameClick: function(game) {
        app.main.setGame(game);
    }

});


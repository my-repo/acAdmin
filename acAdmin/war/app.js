$(document).ready(function(event) {
	APP = new AC.App();
	APP.debug = true;
	window.onbeforeunload = function(){
		APP.ws.close();
	};
	new AC.Main();
});

/** **************************************************************** */

String.prototype.escapeHTML = function() {
	return this.split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;").split("\n").join("<br>");
}

String.prototype.startsWith = function(str) {
	// return (this.match("^"+str)==str)
	return (!str || (this.length >= str.length && this.substring(0, str.length) == str));
}

String.prototype.endsWith = function(str) {
	// return (this.match(str+"$")==str)
	return (!str || (this.length >= str.length && this.substring(this.length - str.length) == str));
}

/** **************************************************************** */

$.Class("AC.HB", {
	param : function(target){
		return target ? target.data("param") : null;
	}
}, {
	//
	init : function() {
		this.t = [];
		this.np = 1;
		this.params = {};
	},

	append : function(text) {
		this.t.push(text);
		return this;
	},

	prepend : function(text) {
		this.t.unshift(text);
		return this;
	},

	param : function(obj) {
		if (!obj)
			return " ";
		this.params["x-" + this.np] = obj;
		return " data-param='x-" + (this.np++) + "' ";
	},

	asString : function(sep) {
		return this.t.join(sep ? sep : "");
	},

	flush : function(jqCont) {
		var html = this.t.join("");
		jqCont.html(html);
		var self = this;
		jqCont.find("[data-param|='x']").each(function() {
			var x = $(this);
			var obj = self.params[x.attr("data-param")];
			x.data("param", obj);
		});
		return jqCont;
	}

});

/** ******************************************************** */
$.Class("AC.PopForm", {
	//
	currentForm : null,
	isOpen : false,
	html1 : "<div class='acBtnFermer' id='fermer'></div>"
		+ "<div id='title' class='acPopFormTitle ac-fontLargeBI'></div>"
		+ "<div id='innerDiv' class='acPopFormDiv ac-fontMedium'>",
	html2 : "</div><div class='acBtn acBtnValider ac-fontLargeBI' id='valider'>"

}, {

	init : function(hb, title, valider) {
		if (!this.constructor._acPopupForm) {
			this.constructor._acPopupForm = $("#acPopupForm");
			this.constructor._acMask = $("#acMask");
		}
		var _content = this.constructor._acPopupForm;
		_content.css("z-index", 202);
		this._content = _content;
		hb.prepend(this.constructor.html1);
		if (valider)
			hb.append(this.constructor.html2).append(valider).append("</div>");
		else
			hb.append("</div>");
		hb.flush(_content);
		if (title)
			_content.find("#title").html(title.escapeHTML());
		if (!this.constructor.currentForm) {
			this.constructor._acMask.css("display", "block");
			var y = APP._work.offset().top + 10;
			_content.css("top", "" + y + "px");
			APP.opacity0(_content);
			_content.css("display", "block");
			setTimeout(function(){
				APP.opacity1(_content);
			}, 30);
		}
		this.constructor.currentForm = this;
		this._innerDiv = _content.find("#innerDiv");
		this._valider = _content.find("#valider");
		APP.oncl(this, "fermer", this.close);
		APP.oncl(this, 	this.constructor._acMask);
	},
		
	repaint : function(t){
		t.flush(this._innerDiv, true);
	},
	
	close : function() {
		var _content = this.constructor._acPopupForm;
		var _mask = this.constructor._acMask;
		APP.opacity0(_content);
		setTimeout(function(){
			_content.html("");
			_content.css("display", "none");
			_mask.css("display", "none");
		}, 300);
		this.constructor.currentForm = null;
	},
		
	enEdition : function() {
		return false;
	},
	
	enErreur : function() {
		return this.error;
	},
		
	enable : function(){
		APP.btnEnable(this._valider, this.enEdition() && !this.enErreur());
	}
	
});

/** ******************************************************** */

$.Class("AC.App", {
	
}, {
	init : function(){
		this.registered = {};
		this.callId = 0;
		
		this._maskA = $("#acMaskA");
		this._attente = $("#acAttente");
		this._work = $("#acWork");
		this.LOG = $("#LOG");
		
		var port = window.location.search.substring(1);
        this.ws = new WebSocket("ws://localhost:" + port);
        this.ws.onopen = function() {
        	if (APP.debug)
        		console.log("[WebSocket#onopen]\n");
        	APP.ready = true;
        	if (APP.onready)
        		APP.onready();
        }
        this.ws.onmessage = function(e) {
  			var exs = null;
  			var msg = null;
      		try {
       	   		msg = $.parseJSON(e.data);
       		} catch(ex){ 
       			exs = ex.toString();
       		}
            if (msg && (msg.type == "log" || msg.type == "err"))
            	APP.log(msg.data, msg.type == "err");
            else {
           		if (APP.debug)
           			console.log("[WebSocket#onmessage] : " + JSON.stringify(msg) + "\n");
           		if (msg.data && msg.data.callId){
           			if (!(msg.data.callId === APP.callId) || !APP.rpcCb)
           				return;
           			APP.unmaskScreen();
           			var cb = APP.rpcCb;
           			APP.rpcCb = null;
           			if (msg.type == "ErrMsg"){
           				APP.log(msg.data.message, true);
       					cb(1, msg.data.message);
           			} else
           				cb(0, msg.data);
           		} else {
	           		var handler = APP.registered[data.type];
	           		if (handler)
	           			handler.onmessage(data);
	           	}
            }
        }
        this.ws.onclose = function() {
        	if (APP.debug)
        		console.log("[WebSocket#onclose]\n");
            this.ws = null;
            close();
        }
	},
	
	register : function(type, handler){
		if (type)
			this.registered[type] = handler;
	},
	
	send : function(type, obj, cb){
		if (cb){
			this.callId++;
			obj.callId = this.callId;
			this.rpcCb = cb;
			this.maskScreen();
		} else
			this.rpcCb = null;
		var arg = JSON.stringify({type:type, data:obj});
        this.ws.send(arg);
	},
		
	maskScreen : function() {
		this._attente.css("display", "none");
		this.attente = false;
		this._maskA.css("display", "block");
		var self = this;
		this.timerAttente = setTimeout(function() {
			self.timerAttente = null;
			self.attente = true;
			var h = $(window).height();
			var w = $(window).width();
			self._attente.css("top", "" + Math.floor((h - 110) / 2) + "px");
			self._attente.css("left", "" + Math.floor((w - 170) / 2) + "px");
			self.opacity0(APP._attente);
			self._attente.css("display", "block");
			self.oncl(self, APP._attente, function(){
				APP.unmaskScreen();
				var cb = APP.rpcCb;
				if (cb) {
					APP.rpcCb = null;
					cb(-1);
				}
			});
			setTimeout(function() {
				self.opacity07(APP._attente);
			}, 20);
		}, 500);
	},

	unmaskScreen : function() {
		if (this.timerAttente) {
			clearTimeout(this.timerAttente);
			this.timerAttente = null;
		}
		if (this.attente) {
			this.attente = false;
			this.opacity0(this._attente);
			var self = this;
			setTimeout(function() {
				self._attente.css("display", "none");
			}, 200);
		}
		this._maskA.css("display", "none");
	},

	log : function(text, err){
		if (text) {
			var cl = err ? "<div class='ac-fontMedium2B acErr'>" : "<div class='ac-fontMedium2'>";
			this.LOG.append(cl + text.escapeHTML() + "</div>");
			this.LOG.scrollTop(this.LOG[0].scrollHeight);
		}
	},
	
	getScroll : function() {
		var x = 0, y = 0;
		if (typeof (window.pageYOffset) == 'number') {
			// Netscape compliant
			y = window.pageYOffset;
			x = window.pageXOffset;
		} else if (document.body && (document.body.scrollLeft || document.body.scrollTop)) {
			// DOM compliant
			y = document.body.scrollTop;
			x = document.body.scrollLeft;
		} else if (document.documentElement
				&& (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
			// IE6 standards compliant mode
			y = document.documentElement.scrollTop;
			x = document.documentElement.scrollLeft;
		}
		var obj = new Object();
		obj.x = x;
		obj.y = y;
		return obj;
	},

	CLICK : "click",
	
	KEYUP : "keyup input",
	
	NOPROPAG : function(event) {
		event.stopPropagation();
	},

	oncl : function(caller, id, cb){
		var idx = typeof id == "string" ? caller._content.find("#" + id) : id;
		idx.off(this.CLICK).on(this.CLICK, {cb:cb, caller:caller}, function(event){
			APP.NOPROPAG(event);
			if (event.data.cb)
				event.data.cb.call(event.data.caller, $(event.currentTarget), event);
		});			
	},

	opacity1 : function(target){
		target.css("opacity", 1);
		target.css("filter", "alpha(opacity=100)")
	},

	opacity0 : function(target){
		target.css("opacity", 0);
		target.css("filter", "alpha(opacity=0)")
	},

	opacity07 : function(target){
		target.css("opacity", 0.7);
		target.css("filter", "alpha(opacity=70)")
	},

	btnEnable : function(cmp, enable) {
		if (!cmp)
			return;
		if (enable)
			cmp.removeClass('ui-disabled');
		else
			cmp.addClass('ui-disabled');
	},
	
	editDH : function(s, p){
		if (s.length < 17)
			s += "00000000000000000";
		var r = s.substring(0,4) + "-" + s.substring(4,6) + "-" + s.substring(6,8) + " " 
			+ s.substring(8,10) + ":" + s.substring(10,12);
		if (p > 0){
			r += ":" + s.substring(12,14);
			if (p > 1){
				r += "." + s.substring(14,17);
			}
		}
		return r;
	},
	
	dateFormat : function(format, date) {
		if (date === undefined) {
			date = new Date();
		}
		if (typeof date == 'number') {
			time = new Date();
			time.setTime(date);
			date = time;
		} else if (typeof date == 'string') {
			date = new Date(date);
		}
		var fullYear = date.getYear();
		if (fullYear < 1000) {
			fullYear = fullYear + 1900;
		}
		var hour = date.getHours();
		var day = date.getDate();
		var month = date.getMonth() + 1;
		var minute = date.getMinutes();
		var seconde = date.getSeconds();
		var ms = date.getMilliseconds();
		var reg = new RegExp('(d|m|Y|H|i|s|S)', 'g');
		var replacement = new Array();
		replacement['d'] = day < 10 ? '0' + day : day;
		replacement['m'] = month < 10 ? '0' + month : month;
		replacement['S'] = ms < 10 ? '00' + ms : (ms < 100 ? '0' + ms : ms);
		replacement['Y'] = fullYear;
		replacement['H'] = hour < 10 ? '0' + hour : hour;
		replacement['i'] = minute < 10 ? '0' + minute : minute;
		replacement['s'] = seconde < 10 ? '0' + seconde : seconde;
		return format.replace(reg, function($0) {
			return ($0 in replacement) ? replacement[$0] : $0.slice(1, $0.length - 1);
		});
	},
	
	hhmmjj : function(date){
		var hour = date.getHours();
		var minute = date.getMinutes();
		var seconde = date.getSeconds();
		return "" + (hour < 10 ? '0' + hour : hour) + ":" 
			+ (minute < 10 ? '0' + minute : minute) + ":" 
			+ (seconde < 10 ? '0' + seconde : seconde);
	},

	stdDateFormat : function(date) {
		return this.dateFormat("Y-m-d H:i:s.S", date)
	},

	stdDateFormat2 : function(date) {
		var x = this.dateFormat("Ymd", date) + "&nbsp;" + this.dateFormat("His.S", date);
		return x.substring(2);
	},

});

/*******************************************************/
AC.PopForm("AC.Test", {
	html : "<div class='ac-fontMediumB acBtn' id='GO1'>GO-une</div>"
		+ "<div class='ac-fontMediumB acBtn' id='GON'>GO-liste</div>"
		
}, {
	init : function(){
		var hb = new AC.HB();
		hb.append(this.constructor.html);
		this._super(hb, "Test PopForm");
		APP.oncl(this, "GO1", function(target){
    		APP.send("Personne", {nom:'Sportes', age:63});
    	});
		APP.oncl(this, "GON", function(){
    		APP.send("Personnes", [{nom:'Sportes', age:63}, {nom:'Colin', age:62}]);
    	});
		APP.register("Personne", this);
		APP.register("Personnes", this);
	},
	
	onmessage : function(data){
		var x = JSON.stringify(data);
		APP.log(x, data.type == "Personne");
	},
	
	close : function(){
		APP.register("Personne");
		APP.register("Personnes");
		this._super();
	}
});

/*******************************************************/
AC.PopForm("AC.Config", {
	
	html : "<table class='acT1'>"
		+ "<tr class='acTR1'><td class='ac-fontMediumBI'>Serveurs</td>"
		+ "<td class='ac-fontMediumBI acTD40'>https://...</td>"
		+ "<td class='ac-fontMediumBI acTD40'>Mot de passe</td>"
		+ "<td class='ac-fontMediumBI'></td></tr>"
		
		+ "<tr class='acTR1'><td class='ac-fontMedium'>Production</td>"
		+ "<td class='acTD40'><input type='text' id='UrlPInp'></input></td>"
		+ "<td class='acTD40'><input type='text' id='PwdPInp'></input></td>"
		+ "<td class='acTDR'><div class='ac-fontMediumB acBtn' id='OkPBtn'>OK</div></td></tr>"

		+ "<tr class='acTR1'><td class='ac-fontMedium'>Test</td>"
		+ "<td class='acTD40'><input type='text' id='UrlTInp'></input></td>"
		+ "<td class='acTD40'><input type='text' id='PwdTInp'></input></td>"
		+ "<td class='acTDR><div class='ac-fontMediumB acBtn' id='OkTBtn'>OK</div></td></tr>"

		+ "<tr class='acTR1'><td class='ac-fontMedium'>Développement</td>"
		+ "<td class='acTD40'><input type='text' id='UrlDInp'></input></td>"
		+ "<td class='acTD40'><input type='text' id='PwdDInp'></input></td>"
		+ "<td class='acTDR><div class='ac-fontMediumB acBtn' id='OkDBtn'>OK</div></td></tr></table>"

		+ "<div class='ac-fontMediumBI'>Répertoire contenant les dumps</div>"
		+ "<div class='ac-fontMediumB acBtn acFLR' id='syncBtn'>Sync</div>"
		+ "<div class='ac-fontMedium' id='dir'></div>"
		+ "<div><div class='acNewdirInp'><input type='text' id='newdirInp'></input></div>"
		+ "<div class='ac-fontMediumB acBtn' id='newdirBtn'>Nouveau</div></div>"
		+ "<div class='ac-fontMedium' id='subdirs'></div>"
		
}, {
	init : function(){
		var hb = new AC.HB();
		hb.append(this.constructor.html);
		this._super(hb, "Configuration");
		this.reload();
		APP.oncl(this, "syncBtn", this.reload);
		this._newdirInp = this._content.find("#newdirInp");
		this._newdirBtn = this._content.find("#newdirBtn");

		this._urlPInp = this._content.find("#UrlPInp");
		this._urlTInp = this._content.find("#UrlTInp");
		this._urlDInp = this._content.find("#UrlDInp");

		this._pwdPInp = this._content.find("#PwdPInp");
		this._pwdTInp = this._content.find("#PwdTInp");
		this._pwdDInp = this._content.find("#PwdDInp");

		this._okPBtn = this._content.find("#OkPBtn");
		this._okTBtn = this._content.find("#OkTBtn");
		this._okDBtn = this._content.find("#OkDBtn");
		
		APP.btnEnable(this._okPBtn, false);
		APP.btnEnable(this._okTBtn, false);
		APP.btnEnable(this._okDBtn, false);
		
		APP.oncl(this, this._newdirBtn, this.newdir);
		var self = this;
		this._newdirInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			APP.NOPROPAG(event);
			if (event.keyCode == 13)
				self.newdir();
			else {
				self.newdirName = self._newdirInp.val();
				APP.btnEnable(self._newdirBtn, self.newdirName);
			}
		});
		this._urlPInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "P");
		});
		this._urlTInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "T");
		});
		this._urlDInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "D");
		});
		this._pwdPInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "P");
		});
		this._pwdTInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "T");
		});
		this._pwdDInp.off(APP.KEYUP).on(APP.KEYUP, function(event){
			self.inpX(event, "D");
		});
		this._okPBtn.off(APP.CLICK).on(APP.CLICK, function(event){
			self.okXBtn(event, "P");
		});
		this._okTBtn.off(APP.CLICK).on(APP.CLICK, function(event){
			self.okXBtn(event, "T");
		});
		this._okDBtn.off(APP.CLICK).on(APP.CLICK, function(event){
			self.okXBtn(event, "D");
		});
	},
	
	okXBtn : function(event, ptd){
		APP.NOPROPAG(event);
		this.updUP(ptd);
	},

	inpX : function(event, ptd){
		APP.NOPROPAG(event);
		if (event.keyCode == 13)
			this.updUP(ptd);
		else {
			this.enBtns();
		}		
	},

	enBtns : function(){
		var uP = this._urlPInp.val() ? this._urlPInp.val() : "";
		var pP = this._pwdPInp.val() ? this._pwdPInp.val() : "";
		APP.btnEnable(this._okPBtn, !(uP === this.config.urlP) || !(pP === this.config.pwdP));
		var uT = this._urlTInp.val() ? this._urlTInp.val() : "";
		var pT = this._pwdTInp.val() ? this._pwdTInp.val() : "";
		APP.btnEnable(this._okTBtn, !(uT === this.config.urlT) || !(pT === this.config.pwdT));
		var uD = this._urlDInp.val() ? this._urlDInp.val() : "";
		var pD = this._pwdDInp.val() ? this._pwdDInp.val() : "";
		APP.btnEnable(this._okDBtn, !(uD === this.config.urlD) || !(pD === this.config.pwdD));
	},
	
	updUP : function(ptd){
		var u = this["_url" + ptd + "Inp"].val();
		var p = this["_pwd" + ptd + "Inp"].val();
		var self = this;
		APP.send("UpdConfigUrl", {url:u, pwd:p, ptd:ptd}, function(err, data){
			if (!err) {
				self.display(data);
			}			
		});
	},
	
	newdir : function(){
		var self = this;
		APP.send("UpdConfigNewDir", {newdir:this.newdirName}, function(err, data){
			if (!err) {
				self.newdirName = "";
				self._newdirInp.val("");
				APP.btnEnable(self._newdirBtn, false);
				self.display(data);
			}
		});
	},
	
	reload : function(){
		var self = this;
		APP.send("ReqConfig", {}, function(err, data){
			if (!err)
				self.display(data);
		});
	},
	
	getSubDirs : function(){
		var self = this;
		APP.send("ReqConfigDirs", {}, function(err, data){
			if (!err)
				self.displayDirs(data);
			else
				self.displayDirs({dirs:[]});
		});	
	},
	
	updConfigDir : function(dir){
		var self = this;
		APP.send("UpdConfigDir", {dir:dir}, function(err, data){
			if (!err)
				self.display(data);
		});			
	},
	
	display : function(data){
		this.config = data;
		this._urlPInp.val(this.config.urlP);
		this._urlTInp.val(this.config.urlT);
		this._urlDInp.val(this.config.urlD);
		this._pwdPInp.val(this.config.pwdP);
		this._pwdTInp.val(this.config.pwdT);
		this._pwdDInp.val(this.config.pwdD);
		this.enBtns();
		var t = new AC.HB();
		t.append("<div class='acPath'>");
		for(var i = 0; i < data.dir.length; i++){
			if (i)
				t.append("<div class='acDirsep ac-fontMedium2'>/</div>");
			t.append("<div class='acDirname ac-fontMedium2B' data-index='" + i + "'>" + data.dir[i] + "</div>");
		}
		t.append("</div>");
		t.flush(this._content.find("#dir"));
		APP.oncl(this, this._content.find(".acDirname"), function(target){
			var n = parseInt(target.attr("data-index"), 10);
			this.updConfigDir(data.dir.slice(0, n + 1));
		});
		this.getSubDirs();
	},

	displayDirs : function(data){
		this.subdirs = data.dirs;
		var t = new AC.HB();
		for(var i = 0; i < data.dirs.length; i++){
			t.append("<div class='acDirname2 ac-fontMedium2B' data-index='" + i + "'>" + data.dirs[i] + "</div>");
		}
		t.flush(this._content.find("#subdirs"));
		APP.oncl(this, this._content.find(".acDirname2"), function(target){
			var n = parseInt(target.attr("data-index"), 10);
			var nd = [];
			for(var i = 0, d = null; d = this.config.dir[i]; i++)
				nd.push(d);
			nd.push(this.subdirs[n]);
			this.updConfigDir(nd);
		});
	},

	onmessage : function(data){
	},
	
	close : function(){
		if (this.config)
			APP.config = this.config;
		this._super();
		AC.Main.current.display();
	}
});

/*******************************************************/
$.Class("AC.Main", {
	onready1 : function(){
		if (!AC.Main.current)
			return;
		AC.Main.current.onready();
	},
	
	lib1 : {P:"Production", T:"Test", D:"Développement"},

	lib2 : {P:"Partiel", C:" "},

	html : "<div class='acLFL'>"
		
		+ "<div class='ac-fontMediumB acBGGris'>Dumps existants</div>"
		+ "<table class='acT2t'>"
		+ "<tr class='acTR1'><td class='ac-fontMediumBI ac33'>Serveurs</td>"
		+ "<td class='ac-fontMediumBI'>Partiel</td>"
		+ "<td class='ac-fontMediumBI ac40'>Date / Heure</td></tr></table>"
		+ "<div class='acT2Cont' id='dumpsC'></div>"
		
		+ "<div class='acSpace2'></div>"
		
		+ "<div class='ac-fontMediumB acBGGris' id='acCurrentDumpT'>(aucun dump sélectionné)</div>"
		+ "<table class='acT2t'>"
		+ "<tr class='acTR1'><td class='ac-fontMediumBI'>Lignes</td>"
		+ "<td class='ac-fontMediumBI ac40'>Date / Heure</td></tr></table>"
		+ "<div class='acT2Cont' id='acCurrentDumpC'></div>"

		+ "<div class='acSpace2'></div>"

		+ "<div class='ac-fontMediumB acBGGris' id='lineT'>(aucune ligne sélectionnée)</div>"
		+ "<table class='acT2t'>"
		+ "<tr class='acTR1'><td class='ac-fontMediumBI ac33'>Colonnes</td>"
		+ "<td class='ac-fontMediumBI'>Type</td>"
		+ "<td class='ac-fontMediumBI ac40'>Date / Heure</td></tr></table>"
		+ "<div class='acT2Cont' id='lineC'></div>"
		
		+ "</div><div class='acRFL'>"
		
		+ "<div class='ac-fontMediumB acBGGrisR' id='colT'>(aucune colonne sélectionnée)</div>"
		+ "<img id='PHOTO' class='acImg ac-fontMediumI'></img>"
		+ "<div id='JSON'>"
		+ "<textarea id='TA' class='acTA'></textarea>"
		+ "<div class='acSpace2'></div>"
		+ "<table class='acT2t'>"
		+ "<tr class='acTR1'><td class='ac-fontMediumBI ac50'>Type d'items</td>"
		+ "<td class='ac-fontMediumBI ac40'>Nombre d'items</td></tr></table>"
		+ "<div class='acT2Cont' id='colC'></div></div>"

		+ "</div><div class='acEnd'></div>"
		
		+ "<div id='itemT'><div class='ac-fontMediumB acBGGris'></div>"
		+ "<textarea class='acTA2'></textarea></div>"
		+ "<div id='itemC'></div>"

}, {
	init : function(){
		this._content = $("body");
		this._work = $("#acWork");
		this.constructor.current = this;
		APP.onready = this.constructor.onready1;
		if (APP.ready)
			this.onready();
	},
	
	onready : function(){
		if (this.onreadyDone)
			return;
		this.onreadyDone = true;
		APP.log("Chargement de la configuration ...");
		this._configuration = $("#configuration");
		APP.oncl(this, this._configuration, function(){
			new AC.Config();
		});
		
		this._test = $("#test");
		APP.oncl(this, this._test, function(){
			new AC.Test();
		});

		var self = this;
		APP.send("ReqConfig", {}, function(err, data){
			if (!err) {
				APP.log("... terminée. Prêt !");
				APP.config = data;
				self.display();
			} else {
				alert(data + "\nLancement de l'application impossible");
				window.close();
			}
		});
		
		this._work.html(this.constructor.html);
		this._titleCD = this._work.find("#acCurrentDumpT");
		this._contCD = this._work.find("#acCurrentDumpC");
		this._contD = this._work.find("#dumpsC");
		this._lineT = this._work.find("#lineT");
		this._lineC = this._work.find("#lineC");
		this._colT = this._work.find("#colT");
		this._colC = this._work.find("#colC");
		this._photo = this._work.find("#PHOTO");
		this._json = this._work.find("#JSON");
		this._ta = this._work.find("#TA");
		this._itemT = this._work.find("#itemT");
		this._itemC = this._work.find("#itemC");
		this.resetCurrentDump();
	},
	
	display : function(){
		APP.btnEnable(this._configuration, true);
		APP.btnEnable(this._test, true);
		this.getSubDirs();
	},
	
	getSubDirs : function(){
		var self = this;
		APP.send("ReqConfigDirs", {}, function(err, data){
			if (!err)
				self.displayDirs(data);
			else
				self.displayDirs({dirs:[]});
		});	
	},

	displayDirs : function(data){
		this.dumps = [];
		var t = new AC.HB();
		t.append("<table class='acT2'>");
		data.dirs.sort();
		for(var i = 0, d = null; d = data.dirs[i]; i++){
			if (d.length != 16)
				continue;
			var ptd = this.constructor.lib1[d.substring(0,1)];
			if (!ptd)
				continue;
			var pc = this.constructor.lib2[d.substring(15)];
			if (!pc)
				continue;
			var dh = APP.editDH(d.substring(1,15), 1);
			this.dumps.push(d);
			t.append("<tr class='acTR1' data-index='" + d + "'>" + "<td class='ac-fontMedium ac33'>" + ptd + "</td>");
			t.append("<td class='ac-fontMedium'>" + pc + "</td>");
			t.append("<td class='ac-fontMedium ac40'>" + dh + "</td></tr>");
		}
		t.append("</table>");
		t.flush(this._contD);
		if (this.currentDump) {
			var path = APP.config.dir.join("/") + "/";
			if (path != this.currentDumpPath || this.dumps.indexOf(this.currentDump) == -1) {
				this.resetCurrentDump();
			} else {
				var c = this._work.find("[data-index='" + this.currentDump + "']");
				c.addClass("acTRSel");
			}
		}
		APP.oncl(this, this._contD.find(".acTR1"), function(target){
			this._contD.find(".acTRSel").removeClass("acTRSel");
			target.addClass("acTRSel");
			this.currentDump = target.attr("data-index");
			this.currentDumpPath = APP.config.dir.join("/") + "/";
			this.setCurrentDump();
		});
	},

	resetCurrentDump : function(){
		this._contD.find(".acTRSel").removeClass("acTRSel");
		this.currentDump = null;
		this.currentDumpPath = null;
		this._titleCD.html("(aucun dump sélectionné)");
		this._contCD.html("");
		this.resetCurrentLine();
	},

	resetCurrentLine : function(){
		this._lineT.html("(aucune ligne sélectionnée)");
		this._lineC.html("");
		this.resetCurrentCol();
	},
	
	resetCurrentCol : function(){
		this._colT.html("(aucune colonne sélectionnée)");
		this._colC.html("");
		this._json.css("display", "none");
		this._photo.css("display", "none");
		this.resetCurrentItem();
	},

	resetCurrentItem : function(){
		this._itemT.css("display", "none");
		this._itemC.css("display", "none");
	},

	setCurrentDump : function(){
		var self = this;
		APP.send("SetCurrentDump", {dump:this.currentDump, path:this.currentDumpPath}, function(err, data){
			if (!err)
				self.displayDump(data);
			else
				self.displayDump({lines:[]});
		});	
	},
	
	displayDump : function(data){
		var dx = this.currentDump;
		var ptd = this.constructor.lib1[dx.substring(0,1)];
		var dh = APP.editDH(dx.substring(1,15), 1);

		this._titleCD.html(ptd + " / " + dh + (dx.substring(15) == "P" ? " (partiel)" : ""));
		this.currentDumpLines = data.lines;
		this.currentDumpLines.sort();
		var t = new AC.HB();
		t.append("<table class='acT2t'>");
		for(var i = 0, d = null; d = this.currentDumpLines[i]; i++){
			var x = this.parseLine(d);
			t.append("<tr class='acTR1' data-index='" + x.id + "'>");
			t.append("<td class='ac-fontMedium'>" + x.line + "</td>");
			t.append("<td class='ac-fontMedium ac40'>" + x.dh + "</td></tr>");
		}
		t.append("</table>");
		t.flush(this._contCD);
		this.resetCurrentLine();
		APP.oncl(this, this._contCD.find(".acTR1"), function(target){
			this._contCD.find(".acTRSel").removeClass("acTRSel");
			target.addClass("acTRSel");
			this.currentLine = this.parseLine(target.attr("data-index"));
			this.setCurrentLine();
		});
	},
	
	parseLine : function(d){
		var j = d.indexOf("_");
		var line = d.substring(0, j);
		var dhx = d.substring(j + 1);
		var dh = APP.editDH(dhx, 2);
		return {id:d, line:line, dh:dh};
	},
	
	setCurrentLine : function(){
		var p = this.currentDumpPath + "/" + this.currentDump;
		var self = this;
		APP.send("SetCurrentZip", {parent:p, zip:this.currentLine.id + ".zip"}, function(err, data){
			if (!err)
				self.displayLine(data);
			else
				self.displayLine({cols:[]});
		});		
	},
	
	displayLine : function(data){
		var x = this.currentLine;
		this._lineT .html(x.line + " / " + x.dh);
		
		this.currentDumpCols = data.cols;
		this.currentDumpCols.sort();
		var t = new AC.HB();
		t.append("<table class='acT2t'>");
		for(var i = 0, d = null; d = this.currentDumpCols[i]; i++){
			var x = this.parseCol(d);
			t.append("<tr class='acTR1' data-index='" + x.id + "'>");
			t.append("<td class='ac-fontMedium ac33'>" + x.col + "</td>");
			t.append("<td class='ac-fontMedium'>" + x.type + "</td>");
			t.append("<td class='ac-fontMedium ac50'>" + x.dh + "</td></tr>");
		}
		t.append("</table>");
		t.flush(this._lineC);
		this.resetCurrentCol();
		APP.oncl(this, this._lineC.find(".acTR1"), function(target){
			this._lineC.find(".acTRSel").removeClass("acTRSel");
			target.addClass("acTRSel");
			this.currentCol = this.parseCol(target.attr("data-index"));
			this.setCurrentCol();
		});
	},
	
	parseCol : function(col){
		var j = col.lastIndexOf(".");
		var x = col.substring(0, j);
		j = col.indexOf("_");
		var c = x.substring(0, j);
		x = x.substring(j + 1);
		j = x.indexOf("_");
		if (j == -1){
			var type = x;
			var dh = " ";
		} else {
			var type = x.substring(0, j);
			var dh = APP.editDH(dhx, 2);
		}
		return{type:type, col:c, dh:dh, id:col}
	},
	
	setCurrentCol : function(){
		var self = this;
		APP.send("GetFromZip", {col:this.currentCol.id}, function(err, data){
			if (!err)
				self.displayCol(data);
			else
				self.displayCol({text:""});
		});		
	},

	displayCol : function(data){
		this._colT.html("Cellule " + this.currentLine.line + " / " + this.currentCol.col + " [" +
				this.currentCol.type + "]");
		if (this.currentCol.type == "PHOTO"){
			this._photo.attr("src", "data:image/jpeg;base64," + data.text);
			this._json.css("display", "none");
			this._itemT.css("display", "none");
			this._itemC.css("display", "none");
			this._photo.css("display", "block");
		} else {
			this._ta.val(data.text);
			this._json.css("display", "block");
			this._photo.css("display", "none");
			this.parseCell(data.text);
		}
	},
	
	parseCell : function(text){
		this.obj = {}
  		try {
   	   		this.obj = $.parseJSON(text);
   		} catch(ex){ 
   			APP.log(ex.toString(), true);
   			return;
   		}
   		var names = [];
   		for(var t in this.obj)
   			names.push(t);
   		names.sort();
   		var t = new AC.HB();
   		t.append("<table class='acT2t'>");
   		for(var i = 0, n = null; n = names[i]; i++){
   			var x = this.obj[n];
   			var nb = Array.isArray(x) ? x.length : 1;
			t.append("<tr class='acTR1' data-index='" + n + "'>");
			t.append("<td class='ac-fontMedium ac50'>" + n + "</td>");
			t.append("<td class='ac-fontMedium ac50'>" + nb + "</td></tr>");
		}
		t.append("</table>");
		t.flush(this._colC);
		this.resetCurrentItem();
		APP.oncl(this, this._colC.find(".acTR1"), function(target){
			this._colC.find(".acTRSel").removeClass("acTRSel");
			target.addClass("acTRSel");
			var n = target.attr("data-index");
   			var x = this.obj[n];
   			var ar = Array.isArray(x) ? x : [x];
			this.displayItems(n, ar);
		});
	},
	
	displayItems : function(name, ar){
		this._itemT.find("textarea").val("");
		var fn = SortKey[name];
		var vect = [];
		for (var i = 0, x = null; x = ar[i]; i++)
			vect.push({ix:i, sk: fn ? fn(x) : 0});
		if (fn)
			vect.sort(function(a,b){
				if (a.sk < b.sk) return -1;
				if (a.sk > b.sk) return 1;
				return 0;
			})
		var fields = {};
		for (var i = 0, x = null; x = ar[i]; i++){
			for(var f in x)
				fields[f] = true;
		}
		var names = [];
		var nx = Columns[name];
		for (var n in fields)
			if (n != "versionDH") {
				if (!nx)
					names.push("9" + n);
				else {
					var k = nx.indexOf(n);
					names.push("" + (k == -1 ? 9 : k) + n);
				}
			}
		names.sort();
		this._itemT.css("display", "block");
		this._itemC.css("display", "block");
		this._itemT.find("div").html("" + ar.length + " item" + (ar.length > 1 ? "s" : "") + " de type " + name);
		var t = new AC.HB();
		t.append("<table class='ac-fontMedium acT3'>");
		t.append("<tr class='ac-fontMediumB'><td class='acC'>#</td>");
		for (var j = 0, name = null; name = names[j]; j++) {
			t.append("<td class='acC'>" + name.substring(1) + "</td>");
		}
		t.append("</tr>");
		for (var i = 0; i < vect.length; i++){
			var x = ar[vect[i].ix];
			t.append("<tr class='ac-fontMedium acTRX" + (i % 2) + "'><td class='acR'>" + (i + 1) + "</td>");
			for (var j = 0, xname = null; xname = names[j]; j++) {
				var name = xname.substring(1);
				var v = x[name];
				var c1 = typeof v == "number" ? "R" : "L";
				t.append("<td class='acTDX ac" + c1 + "' data-index='" + (i*1000 + j) + "'>" + this.editField(v) + "</td>");
			}
			t.append("</tr>");
		}
		t.append("</table>");
		t.flush(this._itemC);
		APP.oncl(this, this._itemC.find(".acTDX"), function(target){
			this._itemC.find(".acTRSel").removeClass("acTRSel");
			target.addClass("acTRSel");
			var n = target.attr("data-index");
			var i = Math.floor(n / 1000);
			var nf = names[n % 1000].substring(1);
   			var v = ar[i][nf];
   			this._itemT.find("textarea").val(v);
		});
	},
	
	editField : function(v){
		if (typeof v == "undefined")
			return "";
		if (typeof v == "number") {
			if (v > 1000000000000)
			 return APP.stdDateFormat2(v);
			return "" + v;
		}
		return v;
	}
});

/****************************************/
$.Class("SortKey",{
	Livr : function(x){
		var gap = x.gap ? x.gap : 0;
		var gac = x.gac ? x.gac : 0;
		var cl = x.codeLivr ? x.codeLivr : 0;
		return (gap * 1000000) + (cl * 1000) + gac;
	},
	Entry : function(x){
		var t = x.type ? x.type : 0;
		var c = x.code ? x.code : 0;
		return (t * 1000000) + c;
	}
},{})

$.Class("Columns",{
	Livr : ["gap", "codeLivr", "gac"],
	Entry : ["type", "code"],
},{})
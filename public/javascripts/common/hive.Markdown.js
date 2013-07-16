/**
 * @(#)hive.Markdown 2013.03.21
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://hive.dev.naver.com/license
 */
hive.Markdown = function(htOptions){

	var htVar = {};
	var htElement = {};
	
	/**
	 * initialize
	 * @param {Hash Table} htOptions
	 */
	function _init(htOptions){
		_initVar(htOptions);
		_initElement(htOptions);
		
		_enableMarkdown();
	}

    /**
     * Return a regular expresion for autolink.
     */
    function _rxLink() {
        // case insensitive match
        var sUserPat = "[a-z0-9-_.]+";
        var sProjectPat = "[-a-z0-9_]+";
        var sNumberPat = "[0-9]+";
        var sShaPat = "[0-9a-f]{7,40}";

        var sProjectPathPat = sUserPat + "/" + sProjectPat;
        var sTargetPat =
            "#(" + sNumberPat + ")|(@)?(" + sShaPat + ")|@(" + sUserPat + ")";

        return new RegExp(
                "(\\S*?)(" + sProjectPathPat + ")?(?:" + sTargetPat + ")(\\S*?)", "gi");
    }

	/**
	 * initialize variables
	 * @param {Hash Table} htOptions
	 */
	function _initVar(htOptions){
		htVar.rxCodeBlock = /```(\w+)(?:\r\n|\r|\n)((\r|\n|.)*?)(\r|\n)```/gm;
        htVar.rxLink = _rxLink();
		htVar.sTplSwitch = htOptions.sTplSwitch;
        htVar.sIssuesUrl = htOptions.sIssuesUrl;
        htVar.sProjectUrl = htOptions.sProjectUrl;

    htVar.htOptSpinner = {
	      lines: 10,    // The number of lines to draw
	      length: 8,    // The length of each line
	      width: 4,     // The line thickness
	      radius: 8,    // The radius of the inner circle
	      corners: 1,   // Corner roundness (0..1)
	      rotate: 0,    // The rotation offset
	      direction: 1, // 1: clockwise, -1: counterclockwise
	      color: '#000',  // #rgb or #rrggbb
	      speed: 1.5,     // Rounds per second
	      trail: 60,      // Afterglow percentage
	      shadow: false,  // Whether to render a shadow
	      hwaccel: false, // Whether to use hardware acceleration
	      className: 'spinner', // The CSS class to assign to the spinner
	      zIndex: 2e9, // The z-index (defaults to 2000000000)
	      top: 'auto', // Top position relative to parent in px
	      left: 'auto' // Left position relative to parent in px
	  };    

	  htVar.htOptMarked = {
		  gfm: true,
		  tables: true,
		  breaks: false,
		  pedantic: false,
		  sanitize: true,
		  smartLists: true,
		  langPrefix: '',
		  highlight: function(code, lang) {
		    if (lang === 'js') {
		      return highlighter.javascript(code);
		    }
		    return code;
		  }
		};
	}
	
	/**
	 * initialize element
     * @param {Hash Table} htOptions
	 */
	function _initElement(htOptions){
		htElement.waTarget = $(htOptions.aTarget) || $("[markdown]");
		htElement.elSpinTarget = document.getElementById('spin');
	}
	
	/**
	 * Render as Markdown document
     * @require showdown.js
     * @require hljs.js
	 * @param {String} sText
	 * @return {String}
	 */
	function _renderMarkdown(sText) {
    var htLexer = new marked.Lexer(htVar.htOptMarked);
		var htTokens = htLexer.lex(sText);
		var sHTML = marked.parser(htTokens);

		return sHTML;
	}

	/**
	 * set Markdown Editor
	 * @param {Wrapped Element} welTextarea
	 */
	function _setEditor(welTextarea) {
		// create new preview area 
		var welPreview = $('<div class="markdown-preview">');
		welPreview.css({
			"display"   : "none",
			"width"     : welTextarea.width()  + 'px',
			"min-height": welTextarea.height() + 'px',
			"padding"   : welTextarea.css("padding")
		});

		var welPreviewSwitch = $('<div id="mode-select">');
			welPreviewSwitch.html(htVar.sTplSwitch);

		var fOnChangeSwitch = function() {
			var bPreview = ($("input:radio[name=edit-mode]:checked").val() == "preview");
			welPreview.html(_renderMarkdown(welTextarea.val()));
			(bPreview ? welPreview: welTextarea).show();
			(bPreview ? welTextarea: welPreview).hide();
		};
		welPreviewSwitch.change(fOnChangeSwitch);

		welTextarea.before(welPreviewSwitch);
		welTextarea.before(welPreview);
	}

	/**
	 * set Markdown Viewer
	 * @param {Wrapped Element} welTarget is not <textarea> or <input>
	 */
	function _setViewer(welTarget) {
		welTarget.html(function() { 
			_startSpinner();
			return _renderMarkdown(welTarget.text());
		}).show('show',_stopSpinner);
	}
	
	/**
	 * enableMarkdown
	 * same as nforge.markdown.enable
	 */
	function _enableMarkdown(){
		var sTagName;
		
		htElement.waTarget.each(function(nIndex, elTarget){
			sTagName = elTarget.tagName.toUpperCase();
			
			if(sTagName == "TEXTAREA" || sTagName == "INPUT" || elTarget.contentEditable == "true"){
				_setEditor($(elTarget));
			} else {
				_setViewer($(elTarget));
			}
		});
	}

	/**
	 * Spinner 시작
	 */
	function _startSpinner(){
    htVar.oSpinner = new Spinner(htVar.htOptSpinner)
    htVar.oSpinner.spin(htElement.elSpinTarget);
	}
	
	/**
	 * Spinner 종료
	 */
	function _stopSpinner(){
    if(htVar.oSpinner){
        htVar.oSpinner.stop();
    }
    htVar.oSpinner = null;
	}

	// initialize
	_init(htOptions || {});
};

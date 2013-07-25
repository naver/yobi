/**
 * @(#)yobi.project.Setting.js 2013.03.18
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 *
 * http://yobi.dev.naver.com/license
 */

(function(ns){

	var oNS = $yobi.createNamespace(ns);
	oNS.container[oNS.name] = function(htOptions){

		var htVar = {};
		var htElement = {};

		/**
		 * initialize
		 */
		function _init(htOptions){
			var htOpt = htOptions || {};
			_initVar(htOpt);
			_initElement(htOpt);
			_attachEvent();
		}

		/**
		 * initialize variables
		 * 정규식 변수는 한번만 선언하는게 성능 향상에 도움이 됩니다
		 */
		function _initVar(htOptions){
            htVar.sURLProjects = htOptions.sURLProjects;
            htVar.sURLMailList = htOptions.sURLMailList;
		}

		/**
		 * initialize element variables
		 */
		function _initElement(htOptions){
            // projects
            htElement.welInputProject = $('#input-project');
            htElement.welSelectedProjects = $('#selected-projects');
            htElement.welBtnSelectProject = $('#select-project');
            htElement.welBtnWriteEmail = $('#write-email');
		}

        /**
		 * attach event handlers
		 */
		function _attachEvent(){
            htElement.welInputProject.keypress(_onKeyPressInputProject);
            htElement.welBtnSelectProject.click(_onClickSelectProject);
            htElement.welBtnWriteEmail.click(_onClickWriteEmail);
            new yobi.ui.Typeahead(htElement.welInputProject, {
		"sActionURL": htVar.sURLProjects
            });
		}

        /**
        * Launch a mail client to write an email.
        */
        function _onClickWriteEmail() {
            // Get project names from labels in #selected-projects div.
            var sMailingType = $('[name=mailingType]:checked').val();
            var waProjectSpan, aProjects;
            if (sMailingType == 'all') {
                aProjects = {'all': 'true'}
            } else {
                waProjectSpan = $('#selected-projects span');
                aProjects = [];
                for (var i = 0; i < waProjectSpan.length; i++) {
                    aProjects.push(waProjectSpan[i].childNodes[0].nodeValue.trim());
                }
            }

            // Send a request contains project names to get email addresses and
            // launch user's mail client with them using mailto scheme.
            htElement.welBtnWriteEmail.button('loading');
            $yobi.sendForm({
                "sURL"      : htVar.sURLMailList,
                "htOptForm": {"method":"POST"},
                "htData"    : aProjects,
                "sDataType" : "json",
                "fOnLoad"   : function(data) {
                    var form = $('<form>');
                    var mailto = 'mailto:';
                    for (var i = 0; i < data.length; i++) {
                        mailto += data[i] + ',';
                    }
                    form.attr('method', 'POST');
                    form.attr('action', mailto);
                    form.attr('enctype', 'text/plain');
                    form.submit();
                    htElement.welBtnWriteEmail.button('reset');
                }
            });
        }

        /**
        * Add a project, which user types in #input-project element, into
        * #selected-projects div.
        */
        function _onClickSelectProject() {
            _appendProjectLabel([htElement.welInputProject.val()]);
            htElement.welInputProject.val("");
            return false;
        }

        /**
        * Same as _onClickSelectProject but triggered by pressing enter.
        *
        * @param {Object} oEvent
        */
        function _onKeyPressInputProject(oEvent) {
            if (oEvent.keyCode == 13) {
                _appendProjectLabel([htElement.welInputProject.val()]);
                htElement.welInputProject.val("");
                return false;
            }
        }

        /**
        * Make a project label by given name.
        *
        * @param {String} sName
        */
        function _createProjectLabel(sName) {
            var fOnClickUnselect = function() {
                welProject.remove();
            };

            var welProject = $('<span class="label label-info">' + sName + " </span>")
            	.append($('<a href="javascript:void(0)">x</a>')
            	.click(fOnClickUnselect));

            return welProject;
        }

        /**
        * Append the given projects on #selected-projects div to show them.
        *
        * @param {Object} htProjects
        */
        function _appendProjectLabel(htTags) {
            for(var sId in htTags) {
                htElement.welSelectedProjects.append(_createProjectLabel(sId, htTags[sId]));
            }
        }

		_init(htOptions);
	};

})("yobi.site.MassMail");
